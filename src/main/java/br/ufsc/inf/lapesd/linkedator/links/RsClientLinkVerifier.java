package br.ufsc.inf.lapesd.linkedator.links;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import javax.annotation.Nullable;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class RsClientLinkVerifier implements LinkVerifier {
    private static final int DEFAULT_CACHE_MAX = 100;
    private static final int DEFAULT_CACHE_TTL = 30;
    private @Nullable Cache<String, Boolean> linkCache;

    public RsClientLinkVerifier(@Nullable Cache<String, Boolean> linkCache) {
        this.linkCache = linkCache;
    }

    public RsClientLinkVerifier() {
        this(CacheBuilder.newBuilder().concurrencyLevel(4)
                .maximumSize(DEFAULT_CACHE_MAX)
                .expireAfterAccess(DEFAULT_CACHE_TTL, TimeUnit.SECONDS)
                .build());
    }

    @Override
    public boolean verify(String link) {
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(link).queryParam("linkedatorOptions", "linkVerify");

        if (linkCache != null) {
            Boolean isValid = linkCache.getIfPresent(link);
            if (isValid != null) return isValid;
        }

        try {
            Response response = webTarget.request().head();
            boolean ok = response.getStatus() == 200;
            linkCache.put(link, ok);
            return ok;
        } catch (Exception e) {
            return false;
        }
    }
}
