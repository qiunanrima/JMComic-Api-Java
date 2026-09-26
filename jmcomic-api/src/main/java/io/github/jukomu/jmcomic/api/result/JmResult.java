package io.github.jukomu.jmcomic.api.result;

import io.github.jukomu.jmcomic.api.exception.JmComicException;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * JMComic API 单次调用的结果载荷。每个接口要么返回带数据的 {@link Success}，
 * 要么返回携带诊断信息的 {@link Failure}。
 * <p>
 * 语义与设计与 picapi 的 PicaResult 完全一致，以大幅降低跨图源适配成本。
 *
 * @param <T> 成功返回的数据类型
 */
public sealed abstract class JmResult<T> implements Serializable permits JmResult.Success, JmResult.Failure {

    public static final class Success<T> extends JmResult<T> {
        private final T data;
        private final int httpCode;

        public Success(T data, int httpCode) {
            this.data = data;
            this.httpCode = httpCode;
        }

        public T getData() {
            return data;
        }

        public int getHttpCode() {
            return httpCode;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public boolean isFailure() {
            return false;
        }

        @Override
        public T getOrNull() {
            return data;
        }

        @Override
        public T getOrDefault(T defaultValue) {
            return data;
        }

        @Override
        public T getOrThrow() {
            return data;
        }

        @Override
        public Failure<T> getFailureOrNull() {
            return null;
        }

        @Override
        public Throwable exceptionOrNull() {
            return null;
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.ofNullable(data);
        }

        @Override
        public JmResult<T> onSuccess(Consumer<? super T> action) {
            Objects.requireNonNull(action).accept(data);
            return this;
        }

        @Override
        public JmResult<T> onFailure(Consumer<? super Failure<?>> action) {
            return this;
        }

        @Override
        public <R> JmResult<R> map(Function<? super T, ? extends R> transform) {
            Objects.requireNonNull(transform);
            try {
                return JmResult.success(transform.apply(data), httpCode);
            } catch (Throwable t) {
                return JmResult.failure(t);
            }
        }

        @Override
        public <R> JmResult<R> flatMap(Function<? super T, ? extends JmResult<R>> transform) {
            Objects.requireNonNull(transform);
            try {
                return transform.apply(data);
            } catch (Throwable t) {
                return JmResult.failure(t);
            }
        }

        @Override
        public <R> R fold(Function<? super T, ? extends R> onSuccess, Function<? super Failure<?>, ? extends R> onFailure) {
            return Objects.requireNonNull(onSuccess).apply(data);
        }

        @Override
        public T getOrElse(Function<? super Failure<?>, ? extends T> fallback) {
            return data;
        }

        @Override
        public String toString() {
            return "JmResult.Success[data=" + data + ", httpCode=" + httpCode + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Success<?> success)) return false;
            return httpCode == success.httpCode && Objects.equals(data, success.data);
        }

        @Override
        public int hashCode() {
            return Objects.hash(data, httpCode);
        }
    }

    public static final class Failure<T> extends JmResult<T> {
        private final Integer httpCode;
        private final String errorCode;
        private final String message;
        private final Throwable cause;

        public Failure(Integer httpCode, String errorCode, String message, Throwable cause) {
            this.httpCode = httpCode;
            this.errorCode = errorCode;
            this.message = message != null ? message : (cause != null ? cause.getMessage() : "Unknown error");
            this.cause = cause;
        }

        public Integer getHttpCode() {
            return httpCode;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getMessage() {
            return message;
        }

        public Throwable getCause() {
            return cause;
        }

        public boolean isNetworkError() {
            return httpCode == null;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public boolean isFailure() {
            return true;
        }

        @Override
        public T getOrNull() {
            return null;
        }

        @Override
        public T getOrDefault(T defaultValue) {
            return defaultValue;
        }

        @Override
        public T getOrThrow() {
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new JmComicException(message, cause);
        }

        @Override
        public Failure<T> getFailureOrNull() {
            return this;
        }

        @Override
        public Throwable exceptionOrNull() {
            return cause != null ? cause : new JmComicException(message);
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.empty();
        }

        @Override
        public JmResult<T> onSuccess(Consumer<? super T> action) {
            return this;
        }

        @Override
        public JmResult<T> onFailure(Consumer<? super Failure<?>> action) {
            Objects.requireNonNull(action).accept(this);
            return this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R> JmResult<R> map(Function<? super T, ? extends R> transform) {
            return (JmResult<R>) this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R> JmResult<R> flatMap(Function<? super T, ? extends JmResult<R>> transform) {
            return (JmResult<R>) this;
        }

        @Override
        public <R> R fold(Function<? super T, ? extends R> onSuccess, Function<? super Failure<?>, ? extends R> onFailure) {
            return Objects.requireNonNull(onFailure).apply(this);
        }

        @Override
        public T getOrElse(Function<? super Failure<?>, ? extends T> fallback) {
            return Objects.requireNonNull(fallback).apply(this);
        }

        @Override
        public String toString() {
            return "JmResult.Failure[httpCode=" + httpCode + ", errorCode=" + errorCode + ", message=" + message + ", cause=" + cause + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Failure<?> failure)) return false;
            return Objects.equals(httpCode, failure.httpCode) &&
                    Objects.equals(errorCode, failure.errorCode) &&
                    Objects.equals(message, failure.message) &&
                    Objects.equals(cause, failure.cause);
        }

        @Override
        public int hashCode() {
            return Objects.hash(httpCode, errorCode, message, cause);
        }
    }

    public abstract boolean isSuccess();
    public abstract boolean isFailure();
    public abstract T getOrNull();
    public abstract T getOrDefault(T defaultValue);
    public T getOrElse(T defaultValue) {
        return getOrDefault(defaultValue);
    }
    public abstract T getOrElse(Function<? super Failure<?>, ? extends T> fallback);
    public abstract T getOrThrow();
    public abstract Failure<T> getFailureOrNull();
    public abstract Throwable exceptionOrNull();
    public abstract Optional<T> toOptional();
    public abstract JmResult<T> onSuccess(Consumer<? super T> action);
    public abstract JmResult<T> onFailure(Consumer<? super Failure<?>> action);
    public abstract <R> JmResult<R> map(Function<? super T, ? extends R> transform);
    public abstract <R> JmResult<R> flatMap(Function<? super T, ? extends JmResult<R>> transform);
    public abstract <R> R fold(Function<? super T, ? extends R> onSuccess, Function<? super Failure<?>, ? extends R> onFailure);

    public static <T> JmResult<T> success(T data) {
        return new Success<>(data, 200);
    }

    public static <T> JmResult<T> success(T data, int httpCode) {
        return new Success<>(data, httpCode);
    }

    public static <T> JmResult<T> failure(Throwable cause) {
        return new Failure<>(null, null, cause != null ? cause.getMessage() : "Unknown error", cause);
    }

    public static <T> JmResult<T> failure(String message) {
        return new Failure<>(null, null, message, null);
    }

    public static <T> JmResult<T> failure(Integer httpCode, String errorCode, String message, Throwable cause) {
        return new Failure<>(httpCode, errorCode, message, cause);
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Throwable;
    }

    /**
     * 执行可抛出异常的操作并包装为 JmResult。
     */
    public static <T> JmResult<T> of(Callable<T> callable) {
        try {
            return success(callable.call());
        } catch (Throwable t) {
            return failure(t);
        }
    }

    /**
     * 执行可抛出异常的操作并包装为 JmResult（Kotlin/现代化命名对齐）。
     */
    public static <T> JmResult<T> runCatching(Callable<T> callable) {
        return of(callable);
    }

    /**
     * 执行无返回值的可抛出异常操作并包装为 JmResult<Void>。
     */
    public static JmResult<Void> runCatching(ThrowingRunnable runnable) {
        try {
            runnable.run();
            return success(null);
        } catch (Throwable t) {
            return failure(t);
        }
    }
}
