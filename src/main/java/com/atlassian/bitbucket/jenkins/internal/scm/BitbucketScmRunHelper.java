package com.atlassian.bitbucket.jenkins.internal.scm;

import com.atlassian.bitbucket.jenkins.internal.status.BitbucketRevisionAction;
import com.atlassian.bitbucket.jenkins.internal.status.BitbucketSCMAction;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;

public final class BitbucketScmRunHelper {

    private BitbucketScmRunHelper() {
    }

    public static boolean hasBitbucketScmOrBitbucketScmSource(Run<?, ?> run) {
        return run.getAction(BitbucketRevisionAction.class) != null || getBitbucketSCMSource(run).isPresent();
    }

    private static Optional<BitbucketSCMSource> getBitbucketSCMSource(Run<?, ?> run) {
        if (run.getParent().getParent() instanceof WorkflowMultiBranchProject) {
            WorkflowMultiBranchProject project = (WorkflowMultiBranchProject) run.getParent().getParent();
            return project.getSCMSources().stream().filter(src -> src instanceof BitbucketSCMSource).map(scmSource -> (BitbucketSCMSource) scmSource).findFirst();
        }
        return empty();
    }

    public static Optional<BitbucketSCM> getBitbucketSCM(Run<?, ?> run) {
        BitbucketSCMAction action = run.getAction(BitbucketSCMAction.class);
        if (action == null) {
            return empty();
        }
        return of(action.getBitbucketSCM());
    }
}
