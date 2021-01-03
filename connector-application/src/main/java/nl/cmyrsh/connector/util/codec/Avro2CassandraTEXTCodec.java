package nl.cmyrsh.connector.util.codec;

import java.nio.ByteBuffer;

import com.datastax.oss.driver.api.core.ProtocolVersion;
import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.DataTypes;
import com.datastax.oss.driver.api.core.type.codec.TypeCodec;
import com.datastax.oss.driver.api.core.type.codec.TypeCodecs;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;

import org.apache.avro.util.Utf8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Avro2CassandraTEXTCodec implements TypeCodec<Utf8> {

    private static final Logger LOG = LoggerFactory.getLogger(Avro2CassandraTEXTCodec.class);

    @Override
    public GenericType<Utf8> getJavaType() {
        return GenericType.of(Utf8.class);
    }

    @Override
    public DataType getCqlType() {
        return DataTypes.TEXT;
    }

    @Override
    public ByteBuffer encode(Utf8 value, ProtocolVersion protocolVersion) {
        LOG.info("Encoding Utf {} to ByteBuffer, ProtocolVersion {}", value, protocolVersion);
        if(null == value) return null;
        return TypeCodecs.TEXT.encode(value.toString(), protocolVersion);
    }

    @Override
    public Utf8 decode(ByteBuffer bytes, ProtocolVersion protocolVersion) {
        LOG.info("Decoding ByteBuffer {} to Utf8", bytes);
        return new Utf8(TypeCodecs.TEXT.decode(bytes, protocolVersion));
    }

    @Override
    public String format(Utf8 value) {
        LOG.info("Formatting Utf8 {}", value);
        return TypeCodecs.TEXT.format(value.toString());
    }

    @Override
    public Utf8 parse(String value) {
        LOG.info("Parsing String {}", value);
        return new Utf8(TypeCodecs.TEXT.parse(value));
    }

    
}
