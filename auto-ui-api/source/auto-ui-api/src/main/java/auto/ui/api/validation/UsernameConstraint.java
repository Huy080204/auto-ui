package auto.ui.api.validation;

import auto.ui.api.constant.AIConstant;
import auto.ui.api.validation.impl.UsernameValidation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UsernameValidation.class)
@Documented
public @interface UsernameConstraint {
    boolean allowNull() default false;

    String pattern() default AIConstant.USERNAME_PATTERN;

    String message() default "Username is invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
