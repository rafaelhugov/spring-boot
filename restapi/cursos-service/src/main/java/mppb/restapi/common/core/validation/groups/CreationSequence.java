package mppb.restapi.common.core.validation.groups;

import jakarta.validation.GroupSequence;
import jakarta.validation.groups.Default;

@GroupSequence({Default.class, Creation.class})
public interface CreationSequence {
}
