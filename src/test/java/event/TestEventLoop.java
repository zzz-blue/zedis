package event;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;




public class TestEventLoop {
    private static EventLoop eventLoop;

    @BeforeClass
    public static void initEventLoop() {
        eventLoop = EventLoop.createEventLoop();
    }

    @Test
    public void testGetNearestTimer() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        for (int i = 0; i < 10; i++) {
//            TimeEvent e = new TimeEvent(i, 10 - i, i * 10, new TimeProcedure() {
//                @Override
//                public int process(Object data) {
//                    System.out.println("....");
//                    return 0;
//                }
//            });
//            eventLoop.registerTimeEvent(e);
        }

        Class<EventLoop> clazz = EventLoop.class;
        Method declaredMethod = clazz.getDeclaredMethod("getNearestTimer");
        declaredMethod.setAccessible(true);
        Object invoke = declaredMethod.invoke(eventLoop);
        declaredMethod.setAccessible(false);

        TimeEvent nearestTimer = (TimeEvent)invoke;
        Assert.assertEquals("1:90", nearestTimer.getWhen() + ":" + nearestTimer.getWhen());
    }
}
