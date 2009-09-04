public class JDK15
{
    public static void main(String[] args)
    {
        if ("".isEmpty()) {
            System.out.println("Test one passed");
        }
        if (!"Foo".isEmpty()) {
            System.out.println("Test two passed");
        }
    }
}