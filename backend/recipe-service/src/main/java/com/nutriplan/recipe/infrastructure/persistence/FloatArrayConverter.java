package com.nutriplan.recipe.infrastructure.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * JPA AttributeConverter: {@code float[]} (Java) <-> {@code byte[]} (columna BLOB).
 * 768 floats * 4 bytes = 3 KB por receta. Little-endian explícito para que sea portable.
 * {@code autoApply = true}: se aplica a todo atributo {@code float[]} sin anotar la entidad.
 */
@Converter(autoApply = true)
public class FloatArrayConverter implements AttributeConverter<float[], byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(float[] vector) {
        if (vector == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        buffer.asFloatBuffer().put(vector);
        return buffer.array();
    }

    @Override
    public float[] convertToEntityAttribute(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        float[] vector = new float[bytes.length / Float.BYTES];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(vector);
        return vector;
    }
}
