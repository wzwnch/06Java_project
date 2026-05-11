package com.shortlink.common.exception;

import com.shortlink.common.enums.BizCodeEnum;
import com.shortlink.common.result.R;
import com.shortlink.utils.AlertUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.sql.SQLException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<Void> handleBizException(BizException e, HttpServletRequest request) {
        log.warn("业务异常 - URI: {}, Method: {}, Code: {}, Message: {}", 
                request.getRequestURI(), request.getMethod(), e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(TokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleTokenException(TokenException e, HttpServletRequest request) {
        log.warn("Token异常 - URI: {}, Method: {}, Code: {}, Message: {}", 
                request.getRequestURI(), request.getMethod(), e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(PermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handlePermissionException(PermissionException e, HttpServletRequest request) {
        log.warn("权限不足 - URI: {}, Method: {}, Code: {}, Message: {}", 
                request.getRequestURI(), request.getMethod(), e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败 - URI: {}, Method: {}, Errors: {}", 
                request.getRequestURI(), request.getMethod(), message);
        
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            String fieldName = fieldError.getField();
            if ("username".equals(fieldName)) {
                return R.fail(BizCodeEnum.USERNAME_FORMAT_ERROR.getCode(), 
                        BizCodeEnum.USERNAME_FORMAT_ERROR.getMessage() + "，需4-20位字母、数字或下划线");
            }
            if ("password".equals(fieldName)) {
                return R.fail(BizCodeEnum.PASSWORD_FORMAT_ERROR.getCode(), 
                        BizCodeEnum.PASSWORD_FORMAT_ERROR.getMessage() + "，需6-20位，包含字母和数字");
            }
        }
        
        return R.fail(400, message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败 - URI: {}, Method: {}, Errors: {}", 
                request.getRequestURI(), request.getMethod(), message);
        return R.fail(400, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("约束校验失败 - URI: {}, Method: {}, Errors: {}", 
                request.getRequestURI(), request.getMethod(), message);
        return R.fail(400, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("缺少必需参数 - URI: {}, Method: {}, Parameter: {}", 
                request.getRequestURI(), request.getMethod(), e.getParameterName());
        return R.fail(400, "缺少必需参数：" + e.getParameterName());
    }

    @ExceptionHandler(TypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleTypeMismatchException(TypeMismatchException e, HttpServletRequest request) {
        log.warn("参数类型转换失败 - URI: {}, Method: {}, Error: {}", 
                request.getRequestURI(), request.getMethod(), e.getMessage());
        return R.fail(400, "参数类型错误");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("请求体解析失败 - URI: {}, Method: {}, Error: {}", 
                request.getRequestURI(), request.getMethod(), e.getMessage());
        return R.fail(400, "请求体格式错误");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public R<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("请求方法不支持 - URI: {}, Method: {}, Supported: {}", 
                request.getRequestURI(), e.getMethod(), e.getSupportedHttpMethods());
        return R.fail(405, "不支持的请求方法：" + e.getMethod());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public R<Void> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        log.warn("媒体类型不支持 - URI: {}, Method: {}, ContentType: {}", 
                request.getRequestURI(), request.getMethod(), e.getContentType());
        return R.fail(415, "不支持的媒体类型");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Void> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("请求路径不存在 - URI: {}, Method: {}", 
                e.getRequestURL(), e.getHttpMethod());
        return R.fail(404, "请求路径不存在");
    }

    @ExceptionHandler({SQLException.class, DataAccessException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleDataAccessException(Exception e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String errorMessage = String.format("数据库访问异常 - URI: %s, Method: %s", uri, method);
        
        log.error("{} - Exception: {}", errorMessage, e.getClass().getName(), e);
        
        AlertUtils.alertDatabaseError(errorMessage, e);
        
        return R.fail(500, "数据库访问失败，请稍后重试");
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleRedisConnectionFailureException(RedisConnectionFailureException e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String errorMessage = String.format("Redis连接失败 - URI: %s, Method: %s", uri, method);
        
        log.error("{} - Exception: {}", errorMessage, e.getClass().getName(), e);
        
        AlertUtils.alertRedisError(errorMessage, e);
        
        return R.fail(500, "缓存服务异常，请稍后重试");
    }

    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String errorMessage = String.format("空指针异常 - URI: %s, Method: %s", uri, method);
        
        log.error("{} - Exception: {}", errorMessage, e.getClass().getName(), e);
        
        return R.fail(500, "系统内部错误，请稍后重试");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数异常 - URI: {}, Method: {}, Message: {}", 
                request.getRequestURI(), request.getMethod(), e.getMessage());
        return R.fail(400, e.getMessage() != null ? e.getMessage() : "参数错误");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String errorMessage = String.format("系统异常 - URI: %s, Method: %s", uri, method);
        
        log.error("{} - Exception: {}", errorMessage, e.getClass().getName(), e);
        
        AlertUtils.alertSystemError(errorMessage, e);
        
        return R.fail(9999, "系统异常，请稍后重试");
    }
}
