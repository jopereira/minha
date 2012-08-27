
libnativetimer.so: pt_minha_kernel_timer_NativeTimer.h NativeTimer.c
	gcc --shared -fPIC -I /usr/java/default/include/ -I /usr/java/default/include/linux/ -I /usr/lib/jvm/java-6-sun/include/ -I /usr/lib/jvm/java-6-sun/include/linux/ NativeTimer.c -o libnativetimer.so -lrt

pt_minha_kernel_timer_NativeTimer.h: ../classes/pt/minha/kernel/timer/NativeTimer.class
	javah -classpath ../classes pt.minha.kernel.timer.NativeTimer 

clean:
	rm -f pt_minha_kernel_timer_NativeTimer.h libnativetimer.so *.o

