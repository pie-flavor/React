package flavor.pie.react;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

import java.math.BigDecimal;

public class BigDecimalSerializer implements TypeSerializer<BigDecimal> {
    @Override
    public BigDecimal deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
        try {
            return new BigDecimal(value.getString());
        } catch (NumberFormatException ex) {
            throw new ObjectMappingException(ex);
        }
    }

    @Override
    public void serialize(TypeToken<?> type, BigDecimal obj, ConfigurationNode value) throws ObjectMappingException {
        value.setValue(obj.toString());
    }
}
