package mppb.restapi.common.core.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.groups.ConvertGroup;
import jakarta.validation.groups.Default;
import mppb.restapi.common.core.validation.groups.Creation;
import mppb.restapi.common.core.validation.groups.Update;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@NotNull(groups = Update.class)
@Null(groups = Creation.class)
public @interface ValidId {
}
