package localhost;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Issue41Bad
{

    public void exceptionMultiCatch() {
        try
        {
            Method method = Issue41Bad.class.getDeclaredMethod( "emptyMethod" );
            method.invoke( null );
        }
        catch ( NoSuchMethodException | IllegalAccessException | InvocationTargetException exception )
        {
            logException( exception );
        }
    }

    public void exceptionClassNotAvailableInJava6()
    {
        try
        {
            Method method = Issue41Bad.class.getDeclaredMethod( "emptyMethod" );
            method.invoke( null );
        }
        catch ( ReflectiveOperationException exception )
        {
            logException( exception );
        }
    }

    public void exceptionMixedCatches() {
        try
        {
            Method method = Issue41Bad.class.getDeclaredMethod( "emptyMethod" );
            method.invoke( null );
        }
        catch ( NoSuchMethodException exception )
        {
            logException( exception );
        }
        catch ( IllegalAccessException | InvocationTargetException exception )
        {
            logException( exception );
        }
        catch ( ReflectiveOperationException exception )
        {
            logException( exception );
        }
    }

    private void logException( Throwable e )
    {
        e.printStackTrace();
    }
}
