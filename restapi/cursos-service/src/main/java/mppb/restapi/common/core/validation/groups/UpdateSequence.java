package mppb.restapi.common.core.validation.groups;

import jakarta.validation.GroupSequence;
import jakarta.validation.groups.Default;

@GroupSequence({Default.class, Update.class})
public class UpdateSequence {
}
