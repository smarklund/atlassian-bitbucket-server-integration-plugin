package com.atlassian.bitbucket.jenkins.internal.jenkins.oauth.servlet;

import com.atlassian.bitbucket.jenkins.internal.applink.oauth.Randomizer;
import com.atlassian.bitbucket.jenkins.internal.applink.oauth.serviceprovider.exception.InvalidTokenException;
import com.atlassian.bitbucket.jenkins.internal.applink.oauth.serviceprovider.token.ServiceProviderToken;
import com.atlassian.bitbucket.jenkins.internal.applink.oauth.serviceprovider.token.ServiceProviderTokenStore;
import com.atlassian.bitbucket.jenkins.internal.applink.oauth.util.OAuthProblemUtils;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Action;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.server.OAuthServlet;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.time.Clock;
import java.util.Map;
import java.util.logging.Logger;

import static com.atlassian.bitbucket.jenkins.internal.applink.oauth.serviceprovider.token.ServiceProviderToken.Authorization;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static jenkins.model.Jenkins.ANONYMOUS;
import static net.oauth.OAuth.*;
import static net.oauth.OAuth.Problems.*;

public class AuthorizeAction extends AbstractDescribableImpl<AuthorizeAction> implements Action {

    private static final Logger LOGGER = Logger.getLogger(AuthorizeAction.class.getName());
    private static final int VERIFIER_LENGTH = 6;

    private ServiceProviderToken serviceProviderToken;
    private String callback;

    public AuthorizeAction(String rawToken, String callback) throws OAuthProblemException {
        serviceProviderToken = getTokenForAuthorization(rawToken);
        this.callback = callback;
    }

    //Used in jelly form target
    @SuppressWarnings("unused")
    public HttpResponse doPerformSubmit(
            StaplerRequest request) throws IOException, ServletException {
        JSONObject data = request.getSubmittedForm();
        Map<String, String[]> params = request.getParameterMap();

        Principal userPrincipal = Jenkins.getAuthentication();
        if (ANONYMOUS.getPrincipal().equals(userPrincipal.getName())) {
            return HttpResponses.error(SC_UNAUTHORIZED, "User not logged in.");
        }

        ServiceProviderToken token;
        try {
            token = getTokenForAuthorization((String) data.get("oauth_token"));
        } catch (OAuthProblemException e) {
            OAuthProblemUtils.logOAuthProblem(OAuthServlet.getMessage(request, null), e, LOGGER);
            return HttpResponses.error(e);
        }

        ServiceProviderToken newToken;
        if (params.containsKey("cancel")) {
            newToken = token.deny(userPrincipal.getName());
        } else if (params.containsKey("authorize")) {
            String verifier = getDescriptor().randomizer.randomAlphanumericString(VERIFIER_LENGTH);
            newToken = token.authorize(userPrincipal.getName(), verifier);
        } else {
            return HttpResponses.error(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
        }
        getDescriptor().tokenStore.put(newToken);

        String callBackUrl =
                addParameters((String) data.get(OAUTH_CALLBACK),
                        OAUTH_TOKEN, newToken.getToken(),
                        OAUTH_VERIFIER,
                        newToken.getAuthorization() == Authorization.AUTHORIZED ? newToken.getVerifier() :
                                "denied");
        return HttpResponses.redirectTo(callBackUrl);
    }

    public String getDisplayName() {
        return "Authorize";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "authorize";
    }

    @SuppressWarnings("unused") //Stapler
    public String getInstanceName() {
        return "Jenkins";
    }

    @SuppressWarnings("unused")
    //Used in Jelly
    public String getAuthenticatedUsername() {
        return Jenkins.getAuthentication().getName();
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return null;
    }

    public String getToken() {
        return serviceProviderToken.getToken();
    }

    @SuppressWarnings("unused")
    //Used in Jelly
    public String getConsumerName() {
        return serviceProviderToken.getConsumer().getName();
    }

    public String getCallback() {
        return callback;
    }

    private ServiceProviderToken getTokenForAuthorization(String rawToken) throws OAuthProblemException {
        ServiceProviderToken token;
        try {
            token = getDescriptor().tokenStore.get(rawToken)
                    .orElseThrow(() -> new OAuthProblemException(TOKEN_REJECTED));
        } catch (InvalidTokenException e) {
            throw new OAuthProblemException(TOKEN_REJECTED);
        }
        if (token.isAccessToken()) {
            throw new OAuthProblemException(TOKEN_REJECTED);
        }
        if (token.getAuthorization() == Authorization.AUTHORIZED ||
            token.getAuthorization() == Authorization.DENIED) {
            throw new OAuthProblemException(TOKEN_USED);
        }
        if (token.hasExpired(getDescriptor().clock)) {
            throw new OAuthProblemException(TOKEN_EXPIRED);
        }
        return token;
    }

    @Override
    public AuthorizeActionDescriptor getDescriptor() {
        return (AuthorizeActionDescriptor) super.getDescriptor();
    }

    @Extension
    @Symbol("authorize-action")
    public static class AuthorizeActionDescriptor extends Descriptor<AuthorizeAction> {

        @Inject
        private ServiceProviderTokenStore tokenStore;
        @Inject
        private Randomizer randomizer;
        @Inject
        private Clock clock;

        public AuthorizeActionDescriptor() {
        }

        @Override
        public AuthorizeAction newInstance(@Nullable StaplerRequest req,
                                           @Nonnull JSONObject formData) throws FormException {
            return createInstance(req);
        }

        public AuthorizeAction createInstance(@Nullable StaplerRequest req) throws FormException {
            try {
                OAuthMessage requestMessage = OAuthServlet.getMessage(req, null);
                requestMessage.requireParameters(OAUTH_TOKEN);
                return new AuthorizeAction(requestMessage.getToken(), requestMessage.getParameter(OAUTH_CALLBACK));
            } catch (OAuthProblemException e) {
                throw new FormException(e, e.getProblem());
            } catch (IOException e) {
                throw new FormException(e, e.getMessage());
            }
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Authorize Action";
        }
    }
}