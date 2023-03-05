package smo_system.util;

public class TakeUtil {
    private TakeUtil() {
        throw new IllegalStateException("Utility class");
    }

    @FunctionalInterface
    public interface Transformer<T> {
        T transform(T object);
    }

    public static <T> T transformOrNull(T object, Transformer<T> transformer){
        return object == null ? null : transformer.transform(object);
    }
}
