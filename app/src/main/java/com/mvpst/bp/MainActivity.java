package com.mvpst.bp;
 
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;

public class MainActivity extends Activity {
	
    private static final int VPN_REQUEST_CODE = 1;
	ShimmerTextView tv;
    Shimmer shimmer;
	
    private Button startButton;
    private Button stopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = (ShimmerTextView) findViewById(R.id.shimmer_tv);
        startButton = (Button) findViewById(R.id.startButton);
        stopButton = (Button) findViewById(R.id.stopButton);
		if (shimmer != null && shimmer.isAnimating()) {
            shimmer = new Shimmer();
            shimmer.start(tv);
        }
        startButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					startVpn();
				}
			});

        stopButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					stopVpn();
				}
			});
    }
	
    private void startVpn() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE);
        } else {
            onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null);
        }
    }

    private void stopVpn() {
        Intent intent = new Intent(this, CoreService.class);
        stopService(intent);
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            Intent vpnIntent = new Intent(this, CoreService.class);
            startService(vpnIntent);
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        }
    }
}
