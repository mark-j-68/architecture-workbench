package com.architectureworkbench.core.model.domain;

import java.util.ArrayList;
import java.util.List;

public class DomainSection {
    private List<BoundedContext> boundedContexts = new ArrayList<>();
    private List<UbiquitousLanguageTerm> ubiquitousLanguage = new ArrayList<>();

    public List<BoundedContext> getBoundedContexts() { return boundedContexts; }
    public void setBoundedContexts(List<BoundedContext> boundedContexts) { this.boundedContexts = boundedContexts == null ? new ArrayList<>() : boundedContexts; }
    public List<UbiquitousLanguageTerm> getUbiquitousLanguage() { return ubiquitousLanguage; }
    public void setUbiquitousLanguage(List<UbiquitousLanguageTerm> ubiquitousLanguage) { this.ubiquitousLanguage = ubiquitousLanguage == null ? new ArrayList<>() : ubiquitousLanguage; }
}
