public class CharType extends BaseType {
    private char value;

    public CharType(){
        value = 0;
    }

    public CharType(char c){
        value = c;
    }

    public char getValue() {
        return value;
    }

    public void setValue(char value) {
        this.value = value;
    }
}