package com.bsu.cvbuilder.domain.event;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AgreementEvent extends AbstractEvent{

    public AgreementEvent(String userId) {
        super(userId);
    }
}
