package com.java6.springboot.internflow.handle;

import com.java6.springboot.internflow.dto.ApiResponse;
import com.java6.springboot.internflow.exception.BusinessException;
import com.java6.springboot.internflow.exception.NotFoundException;
import java.time.Instant;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, exception.getMessage(), null, Instant.now()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, exception.getMessage(), null, Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage() == null ? error.getField() + " khong hop le" : error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, message.isBlank() ? "Du lieu gui len chua hop le" : message, null, Instant.now()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Du lieu gui len khong dung dinh dang. Vui long thu lai.", null, Instant.now()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException exception) {
        log.warn("Data integrity violation", exception);
        String message = persistenceMessage(exception);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(false, message, null, Instant.now()));
    }

    @ExceptionHandler({JpaSystemException.class, TransactionSystemException.class})
    public ResponseEntity<ApiResponse<Void>> handlePersistenceSystem(Exception exception) {
        log.warn("Persistence system exception", exception);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(false, persistenceMessage(exception), null, Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        log.error("Unhandled exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "He thong dang gap su co ngoai du kien. Vui long thu lai sau vai giay.", null, Instant.now()));
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "" : current.getMessage();
    }

    private String persistenceMessage(Throwable throwable) {
        String detail = rootCauseMessage(throwable).toLowerCase();
        if (detail.contains("uk_attendance_user_shift_date")) {
            return "Ban da checkin ca nay trong ngay roi.";
        }
        if (detail.contains("uk_attendance_image_slot")) {
            return "Moc anh nay da ton tai. He thong se cap nhat lai anh moi neu ban thu lai.";
        }
        if (detail.contains("not-null") && detail.contains("group_image")) {
            return "Anh nhom la tuy chon, nhung du lieu cu tren he thong dang chua dong bo. Vui long tai lai trang va thu lai.";
        }
        if (detail.contains("value too long") || detail.contains("too long for type character varying(500)")) {
            return "Du lieu anh hoac ghi chu qua dai de luu. Vui long thu lai voi du lieu ngan hon.";
        }
        return "Khong the luu du lieu vi thong tin bi trung hoac chua hop le.";
    }
}
