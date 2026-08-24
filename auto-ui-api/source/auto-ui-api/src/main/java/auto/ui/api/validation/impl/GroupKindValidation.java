package auto.ui.api.validation.impl;

import auto.ui.api.constant.AIConstant;
import auto.ui.api.validation.GroupKind;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Objects;

public class GroupKindValidation implements ConstraintValidator<GroupKind, Integer> {
    private boolean allowNull;

    @Override
    public void initialize(GroupKind constraintAnnotation) {
        allowNull = constraintAnnotation.allowNull();
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext constraintValidatorContext) {
        if (value == null && allowNull) {
            return true;
        }
        return Objects.equals(value, AIConstant.GROUP_KIND_ADMIN)
                || Objects.equals(value, AIConstant.GROUP_KIND_MENTOR)
                || Objects.equals(value, AIConstant.GROUP_KIND_STUDENT);
    }
}