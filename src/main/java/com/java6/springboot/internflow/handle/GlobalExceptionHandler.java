package com.java6.springboot.internflow.handle;

import com.java6.springboot.internflow.dto.ApiResponse;
import com.java6.springboot.internflow.exception.BusinessException;
import com.java6.springboot.internflow.exception.ForbiddenException;
import com.java6.springboot.internflow.exception.NotFoundException;
import com.java6.springboot.internflow.exception.UnauthorizedException;
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

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(false, exception.getMessage(), null, Instant.now()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(false, exception.getMessage(), null, Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage() == null ? error.getField() + " không hợp lệ" : error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, message.isBlank() ? "Dữ liệu gửi lên chưa hợp lệ." : message, null, Instant.now()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Dữ liệu gửi lên không đúng định dạng. Vui lòng thử lại.", null, Instant.now()));
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
                .body(new ApiResponse<>(false, "Hệ thống đang gặp sự cố ngoài dự kiến. Vui lòng thử lại sau vài giây.", null, Instant.now()));
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
            return "Bạn đã check-in ca này trong ngày rồi.";
        }
        if (detail.contains("uk_attendance_image_slot")) {
            return "Mốc ảnh này đã tồn tại. Hệ thống sẽ cập nhật lại ảnh mới nếu bạn thử lại.";
        }
        if (detail.contains("not-null") && detail.contains("group_image")) {
            return "Ảnh nhóm là tùy chọn, nhưng dữ liệu cũ trên hệ thống chưa đồng bộ. Vui lòng tải lại trang và thử lại.";
        }
        if (detail.contains("value too long") || detail.contains("too long for type character varying(500)")) {
            return "Dữ liệu ảnh hoặc ghi chú quá dài để lưu. Vui lòng thử lại với dữ liệu ngắn hơn.";
        }
        return "Không thể lưu dữ liệu vì thông tin bị trùng hoặc chưa hợp lệ.";
    }
}
