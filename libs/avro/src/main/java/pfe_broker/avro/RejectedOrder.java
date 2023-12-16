/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package pfe_broker.avro;

import org.apache.avro.generic.GenericArray;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.util.Utf8;
import org.apache.avro.message.BinaryMessageEncoder;
import org.apache.avro.message.BinaryMessageDecoder;
import org.apache.avro.message.SchemaStore;

@org.apache.avro.specific.AvroGenerated
public class RejectedOrder extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = 484513667864573901L;


  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"RejectedOrder\",\"namespace\":\"pfe_broker.avro\",\"fields\":[{\"name\":\"order\",\"type\":{\"type\":\"record\",\"name\":\"Order\",\"fields\":[{\"name\":\"username\",\"type\":\"string\"},{\"name\":\"symbol\",\"type\":\"string\"},{\"name\":\"quantity\",\"type\":\"int\"},{\"name\":\"side\",\"type\":{\"type\":\"enum\",\"name\":\"Side\",\"symbols\":[\"BUY\",\"SELL\"]}},{\"name\":\"type\",\"type\":{\"type\":\"enum\",\"name\":\"Type\",\"symbols\":[\"MARKET\",\"LIMIT\"]},\"default\":\"MARKET\"},{\"name\":\"price\",\"type\":[\"null\",\"double\"],\"default\":null},{\"name\":\"clOrderID\",\"type\":\"string\",\"default\":\"\"}]}},{\"name\":\"reason\",\"type\":{\"type\":\"enum\",\"name\":\"OrderRejectReason\",\"symbols\":[\"BROKER_EXCHANGE_OPTION\",\"UNKNOWN_SYMBOL\",\"EXCHANGE_CLOSED\",\"ORDER_EXCEEDS_LIMIT\",\"TOO_LATE_TO_ENTER\",\"UNKNOWN_ORDER\",\"DUPLICATE_ORDER\",\"STALE_ORDER\",\"INCORRECT_QUANTITY\",\"UNKNOWN_ACCOUNT\",\"PRICE_EXCEEDS_CURRENT_PRICE_BAND\",\"OTHER\"]}}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }

  private static final SpecificData MODEL$ = new SpecificData();

  private static final BinaryMessageEncoder<RejectedOrder> ENCODER =
      new BinaryMessageEncoder<>(MODEL$, SCHEMA$);

  private static final BinaryMessageDecoder<RejectedOrder> DECODER =
      new BinaryMessageDecoder<>(MODEL$, SCHEMA$);

  /**
   * Return the BinaryMessageEncoder instance used by this class.
   * @return the message encoder used by this class
   */
  public static BinaryMessageEncoder<RejectedOrder> getEncoder() {
    return ENCODER;
  }

  /**
   * Return the BinaryMessageDecoder instance used by this class.
   * @return the message decoder used by this class
   */
  public static BinaryMessageDecoder<RejectedOrder> getDecoder() {
    return DECODER;
  }

  /**
   * Create a new BinaryMessageDecoder instance for this class that uses the specified {@link SchemaStore}.
   * @param resolver a {@link SchemaStore} used to find schemas by fingerprint
   * @return a BinaryMessageDecoder instance for this class backed by the given SchemaStore
   */
  public static BinaryMessageDecoder<RejectedOrder> createDecoder(SchemaStore resolver) {
    return new BinaryMessageDecoder<>(MODEL$, SCHEMA$, resolver);
  }

  /**
   * Serializes this RejectedOrder to a ByteBuffer.
   * @return a buffer holding the serialized data for this instance
   * @throws java.io.IOException if this instance could not be serialized
   */
  public java.nio.ByteBuffer toByteBuffer() throws java.io.IOException {
    return ENCODER.encode(this);
  }

  /**
   * Deserializes a RejectedOrder from a ByteBuffer.
   * @param b a byte buffer holding serialized data for an instance of this class
   * @return a RejectedOrder instance decoded from the given buffer
   * @throws java.io.IOException if the given bytes could not be deserialized into an instance of this class
   */
  public static RejectedOrder fromByteBuffer(
      java.nio.ByteBuffer b) throws java.io.IOException {
    return DECODER.decode(b);
  }

  private pfe_broker.avro.Order order;
  private pfe_broker.avro.OrderRejectReason reason;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public RejectedOrder() {}

  /**
   * All-args constructor.
   * @param order The new value for order
   * @param reason The new value for reason
   */
  public RejectedOrder(pfe_broker.avro.Order order, pfe_broker.avro.OrderRejectReason reason) {
    this.order = order;
    this.reason = reason;
  }

  @Override
  public org.apache.avro.specific.SpecificData getSpecificData() { return MODEL$; }

  @Override
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }

  // Used by DatumWriter.  Applications should not call.
  @Override
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return order;
    case 1: return reason;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  // Used by DatumReader.  Applications should not call.
  @Override
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: order = (pfe_broker.avro.Order)value$; break;
    case 1: reason = (pfe_broker.avro.OrderRejectReason)value$; break;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  /**
   * Gets the value of the 'order' field.
   * @return The value of the 'order' field.
   */
  public pfe_broker.avro.Order getOrder() {
    return order;
  }


  /**
   * Sets the value of the 'order' field.
   * @param value the value to set.
   */
  public void setOrder(pfe_broker.avro.Order value) {
    this.order = value;
  }

  /**
   * Gets the value of the 'reason' field.
   * @return The value of the 'reason' field.
   */
  public pfe_broker.avro.OrderRejectReason getReason() {
    return reason;
  }


  /**
   * Sets the value of the 'reason' field.
   * @param value the value to set.
   */
  public void setReason(pfe_broker.avro.OrderRejectReason value) {
    this.reason = value;
  }

  /**
   * Creates a new RejectedOrder RecordBuilder.
   * @return A new RejectedOrder RecordBuilder
   */
  public static pfe_broker.avro.RejectedOrder.Builder newBuilder() {
    return new pfe_broker.avro.RejectedOrder.Builder();
  }

  /**
   * Creates a new RejectedOrder RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new RejectedOrder RecordBuilder
   */
  public static pfe_broker.avro.RejectedOrder.Builder newBuilder(pfe_broker.avro.RejectedOrder.Builder other) {
    if (other == null) {
      return new pfe_broker.avro.RejectedOrder.Builder();
    } else {
      return new pfe_broker.avro.RejectedOrder.Builder(other);
    }
  }

  /**
   * Creates a new RejectedOrder RecordBuilder by copying an existing RejectedOrder instance.
   * @param other The existing instance to copy.
   * @return A new RejectedOrder RecordBuilder
   */
  public static pfe_broker.avro.RejectedOrder.Builder newBuilder(pfe_broker.avro.RejectedOrder other) {
    if (other == null) {
      return new pfe_broker.avro.RejectedOrder.Builder();
    } else {
      return new pfe_broker.avro.RejectedOrder.Builder(other);
    }
  }

  /**
   * RecordBuilder for RejectedOrder instances.
   */
  @org.apache.avro.specific.AvroGenerated
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<RejectedOrder>
    implements org.apache.avro.data.RecordBuilder<RejectedOrder> {

    private pfe_broker.avro.Order order;
    private pfe_broker.avro.Order.Builder orderBuilder;
    private pfe_broker.avro.OrderRejectReason reason;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$, MODEL$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(pfe_broker.avro.RejectedOrder.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.order)) {
        this.order = data().deepCopy(fields()[0].schema(), other.order);
        fieldSetFlags()[0] = other.fieldSetFlags()[0];
      }
      if (other.hasOrderBuilder()) {
        this.orderBuilder = pfe_broker.avro.Order.newBuilder(other.getOrderBuilder());
      }
      if (isValidValue(fields()[1], other.reason)) {
        this.reason = data().deepCopy(fields()[1].schema(), other.reason);
        fieldSetFlags()[1] = other.fieldSetFlags()[1];
      }
    }

    /**
     * Creates a Builder by copying an existing RejectedOrder instance
     * @param other The existing instance to copy.
     */
    private Builder(pfe_broker.avro.RejectedOrder other) {
      super(SCHEMA$, MODEL$);
      if (isValidValue(fields()[0], other.order)) {
        this.order = data().deepCopy(fields()[0].schema(), other.order);
        fieldSetFlags()[0] = true;
      }
      this.orderBuilder = null;
      if (isValidValue(fields()[1], other.reason)) {
        this.reason = data().deepCopy(fields()[1].schema(), other.reason);
        fieldSetFlags()[1] = true;
      }
    }

    /**
      * Gets the value of the 'order' field.
      * @return The value.
      */
    public pfe_broker.avro.Order getOrder() {
      return order;
    }


    /**
      * Sets the value of the 'order' field.
      * @param value The value of 'order'.
      * @return This builder.
      */
    public pfe_broker.avro.RejectedOrder.Builder setOrder(pfe_broker.avro.Order value) {
      validate(fields()[0], value);
      this.orderBuilder = null;
      this.order = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'order' field has been set.
      * @return True if the 'order' field has been set, false otherwise.
      */
    public boolean hasOrder() {
      return fieldSetFlags()[0];
    }

    /**
     * Gets the Builder instance for the 'order' field and creates one if it doesn't exist yet.
     * @return This builder.
     */
    public pfe_broker.avro.Order.Builder getOrderBuilder() {
      if (orderBuilder == null) {
        if (hasOrder()) {
          setOrderBuilder(pfe_broker.avro.Order.newBuilder(order));
        } else {
          setOrderBuilder(pfe_broker.avro.Order.newBuilder());
        }
      }
      return orderBuilder;
    }

    /**
     * Sets the Builder instance for the 'order' field
     * @param value The builder instance that must be set.
     * @return This builder.
     */

    public pfe_broker.avro.RejectedOrder.Builder setOrderBuilder(pfe_broker.avro.Order.Builder value) {
      clearOrder();
      orderBuilder = value;
      return this;
    }

    /**
     * Checks whether the 'order' field has an active Builder instance
     * @return True if the 'order' field has an active Builder instance
     */
    public boolean hasOrderBuilder() {
      return orderBuilder != null;
    }

    /**
      * Clears the value of the 'order' field.
      * @return This builder.
      */
    public pfe_broker.avro.RejectedOrder.Builder clearOrder() {
      order = null;
      orderBuilder = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'reason' field.
      * @return The value.
      */
    public pfe_broker.avro.OrderRejectReason getReason() {
      return reason;
    }


    /**
      * Sets the value of the 'reason' field.
      * @param value The value of 'reason'.
      * @return This builder.
      */
    public pfe_broker.avro.RejectedOrder.Builder setReason(pfe_broker.avro.OrderRejectReason value) {
      validate(fields()[1], value);
      this.reason = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'reason' field has been set.
      * @return True if the 'reason' field has been set, false otherwise.
      */
    public boolean hasReason() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'reason' field.
      * @return This builder.
      */
    public pfe_broker.avro.RejectedOrder.Builder clearReason() {
      reason = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RejectedOrder build() {
      try {
        RejectedOrder record = new RejectedOrder();
        if (orderBuilder != null) {
          try {
            record.order = this.orderBuilder.build();
          } catch (org.apache.avro.AvroMissingFieldException e) {
            e.addParentField(record.getSchema().getField("order"));
            throw e;
          }
        } else {
          record.order = fieldSetFlags()[0] ? this.order : (pfe_broker.avro.Order) defaultValue(fields()[0]);
        }
        record.reason = fieldSetFlags()[1] ? this.reason : (pfe_broker.avro.OrderRejectReason) defaultValue(fields()[1]);
        return record;
      } catch (org.apache.avro.AvroMissingFieldException e) {
        throw e;
      } catch (java.lang.Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumWriter<RejectedOrder>
    WRITER$ = (org.apache.avro.io.DatumWriter<RejectedOrder>)MODEL$.createDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumReader<RejectedOrder>
    READER$ = (org.apache.avro.io.DatumReader<RejectedOrder>)MODEL$.createDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

  @Override protected boolean hasCustomCoders() { return true; }

  @Override public void customEncode(org.apache.avro.io.Encoder out)
    throws java.io.IOException
  {
    this.order.customEncode(out);

    out.writeEnum(this.reason.ordinal());

  }

  @Override public void customDecode(org.apache.avro.io.ResolvingDecoder in)
    throws java.io.IOException
  {
    org.apache.avro.Schema.Field[] fieldOrder = in.readFieldOrderIfDiff();
    if (fieldOrder == null) {
      if (this.order == null) {
        this.order = new pfe_broker.avro.Order();
      }
      this.order.customDecode(in);

      this.reason = pfe_broker.avro.OrderRejectReason.values()[in.readEnum()];

    } else {
      for (int i = 0; i < 2; i++) {
        switch (fieldOrder[i].pos()) {
        case 0:
          if (this.order == null) {
            this.order = new pfe_broker.avro.Order();
          }
          this.order.customDecode(in);
          break;

        case 1:
          this.reason = pfe_broker.avro.OrderRejectReason.values()[in.readEnum()];
          break;

        default:
          throw new java.io.IOException("Corrupt ResolvingDecoder.");
        }
      }
    }
  }
}










