package me.megamichiel.animatedmenu.util;

public interface Supplier<T> {

    T get();

    class ConstantSupplier<T> implements Supplier<T> {

        public static <T> ConstantSupplier<T> of(T value) {
            return new ConstantSupplier<>(value);
        }

        private final T value;

        public ConstantSupplier(T value) {
            this.value = value;
        }

        @Override
        public T get() {
            return value;
        }
    }
}
