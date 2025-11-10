package com.toll.edge.exception;

import lombok.Getter;

@Getter
public class ManualInterventionRequiredException extends RuntimeException {

    private final String tagId;
    private final String reason;
    private final String actionMessage;

    public ManualInterventionRequiredException(String tagId, String reason, String actionMessage) {
        super(reason);
        this.tagId = tagId;
        this.reason = reason;
        this.actionMessage = actionMessage;
    }
}
