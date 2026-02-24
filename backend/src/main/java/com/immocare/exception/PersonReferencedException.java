package com.immocare.exception;

import java.util.List;

public class PersonReferencedException extends RuntimeException {

    private final List<String> ownedBuildings;
    private final List<String> ownedUnits;
    private final List<String> activeLeases;

    public PersonReferencedException(Long personId,
                                     List<String> ownedBuildings,
                                     List<String> ownedUnits,
                                     List<String> activeLeases) {
        super("Person " + personId + " cannot be deleted because they are still referenced.");
        this.ownedBuildings = ownedBuildings;
        this.ownedUnits = ownedUnits;
        this.activeLeases = activeLeases;
    }

    public List<String> getOwnedBuildings() { return ownedBuildings; }
    public List<String> getOwnedUnits() { return ownedUnits; }
    public List<String> getActiveLeases() { return activeLeases; }
}
