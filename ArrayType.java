public class ArrayType extends BaseType {
    private BaseType[] value;

    public ArrayType() {
        value = null;
    }

    public ArrayType(BaseType[] array) {
        value = array;
    }

    public BaseType[] getValue() {
        return value;
    }

    public void setValue(BaseType[] value) {
        this.value = value;
    }
}