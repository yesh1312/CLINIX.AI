package com.clinixai.llm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LLMProviderFactory {

    @Value("${clinixai.llm.active-provider:mock}")
    private String activeProviderName;

    private final Map<String, LLMProvider> providers = new ConcurrentHashMap<>();

    @Autowired
    private List<LLMProvider> providerList;

    @PostConstruct
    public void init() {
        for (LLMProvider p : providerList) {
            providers.put(p.getName().toLowerCase(), p);
        }
    }

    public LLMProvider getActiveProvider() {
        return providers.getOrDefault(activeProviderName.toLowerCase(), providers.get("mock"));
    }

    public LLMProvider getProvider(String name) {
        return providers.get(name.toLowerCase());
    }
}
