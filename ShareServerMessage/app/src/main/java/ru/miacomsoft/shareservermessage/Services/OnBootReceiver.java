package ru.miacomsoft.shareservermessage.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import ru.miacomsoft.shareservermessage.MainActivity;


public class OnBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
            Toast.makeText(context, "Boot Signal Server", Toast.LENGTH_SHORT).show();

            //  запуск сервиса
            //  Intent serviceLauncher = new Intent(context, ServiceExample.class);
            //context.startService(serviceLauncher);

            //  Intent serviceLauncher = new Intent(context, MainActivity.class);
            //  serviceLauncher.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //  context.startActivity(serviceLauncher);
            //  Toast.makeText(context, "Boot Signal Server", Toast.LENGTH_SHORT).show();

           /*
            HashMap<String, String> Setup = Sys.readFile(context, "conf.ini");
            if (Setup != null) {
               // if (Setup.get("run").equals("1")) {
                     //Intent serviceLauncher = new Intent(context, ServiceExample.class);
                    //context.startService(serviceLauncher);
                     Intent serviceLauncher = new Intent(context, MainActivity.class);
                     context.startService(serviceLauncher);
               // }
            }
            */
        }
    }
}
