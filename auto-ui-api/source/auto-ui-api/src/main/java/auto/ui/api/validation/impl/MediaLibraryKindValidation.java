package auto.ui.api.validation.impl;

import auto.ui.api.constant.AIConstant;
import auto.ui.api.validation.MediaLibraryKind;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class MediaLibraryKindValidation implements ConstraintValidator<MediaLibraryKind, Integer> {

    private boolean allowNull;

    @Override
    public void initialize(MediaLibraryKind constraintAnnotation) {
        allowNull = constraintAnnotation.allowNull();
    }

    @Override
    public boolean isValid(Integer mediaLibraryKind, ConstraintValidatorContext constraintValidatorContext) {
        if (mediaLibraryKind == null) {
            return allowNull;
        }
        return AIConstant.MEDIA_LIBRARY_KIND_IMAGE.equals(mediaLibraryKind)
                || AIConstant.MEDIA_LIBRARY_KIND_ICON.equals(mediaLibraryKind);
    }
}
