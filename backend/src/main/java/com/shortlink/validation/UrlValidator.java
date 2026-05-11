package com.shortlink.validation;

import cn.hutool.core.util.StrUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class UrlValidator implements ConstraintValidator<Url, String> {

    private static final Pattern URL_PATTERN = Pattern.compile("^https?://[\\w\\-]+(\\.[\\w\\-]+)+[/#?]?.*$");

    private boolean required;
    private int maxLength;

    @Override
    public void initialize(Url constraintAnnotation) {
        this.required = constraintAnnotation.required();
        this.maxLength = constraintAnnotation.maxLength();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StrUtil.isBlank(value)) {
            return !required;
        }

        if (value.length() > maxLength) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("URL长度不能超过" + maxLength + "个字符")
                    .addConstraintViolation();
            return false;
        }

        return URL_PATTERN.matcher(value).matches();
    }
}
