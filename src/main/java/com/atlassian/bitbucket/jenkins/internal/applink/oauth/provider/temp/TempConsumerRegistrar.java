package com.atlassian.bitbucket.jenkins.internal.applink.oauth.provider.temp;

import com.atlassian.bitbucket.jenkins.internal.applink.oauth.common.Consumer;
import com.atlassian.bitbucket.jenkins.internal.applink.oauth.provider.ConsumerStore;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TempConsumerRegistrar {

    private final ConsumerStore consumerStore;

    @Inject
    public TempConsumerRegistrar(
            ConsumerStore consumerStore) {
        this.consumerStore = consumerStore;
    }

    public void registerConsumer(String consumerKey, String consumerSecret) {
        Consumer consumer = consumerStore.get(consumerKey);
        if (consumer == null) {
            consumerStore.add(Consumer.key(consumerKey).name(consumerKey).signatureMethod(Consumer.SignatureMethod.HMAC_SHA1).consumerSecret(consumerSecret).build());
        }
    }
}