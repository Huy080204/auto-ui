package auto.ui.api.validation.impl;

import auto.ui.api.constant.AIConstant;
import auto.ui.api.validation.PasswordConstraint;
import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class PasswordValidation implements ConstraintValidator<PasswordConstraint, String> {
    private boolean allowNull;

    @Override
    public void initialize(PasswordConstraint constraintAnnotation) {
        allowNull = constraintAnnotation.allowNull();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        return StringUtils.isBlank(value) ? allowNull : StringUtils.isNotBlank(value) && value.matches(AIConstant.PASSWORD_PATTERN);
    }
}
