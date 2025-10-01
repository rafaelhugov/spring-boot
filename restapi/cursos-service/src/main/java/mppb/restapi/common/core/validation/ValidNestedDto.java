package mppb.restapi.common.core.validation;

import jakarta.validation.Valid;
import jakarta.validation.groups.ConvertGroup;
import jakarta.validation.groups.Default;
import mppb.restapi.common.core.validation.groups.Creation;
import mppb.restapi.common.core.validation.groups.Update;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Valid
@ConvertGroup.List({
        @ConvertGroup(from = Creation.class, to = Update.class),
        @ConvertGroup(from = Default.class, to = Update.class)
})
public @interface ValidNestedDto {
}
