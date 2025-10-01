package mppb.restapi.common.core.validation.groups;

import jakarta.validation.GroupSequence;
import jakarta.validation.groups.Default;

@GroupSequence({Default.class, Delete.class})
public class DeleteSequence {
}
