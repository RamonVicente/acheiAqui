package acheiaqui.com.acheiaqui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

public class RegisterLocationActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private Marker currentLocationMaker;
    private LatLng currentLocationLatLong;
    private LocationData locationData = new LocationData();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_location);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        startGettingLocations();

        Button btn = (Button) findViewById(R.id.btn_locationRegister);

        //btn.setOnClickListener(new View.OnClickListener);
    }

    //funcao que carrega o mapa quando o aplicativo e aberto
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    //funcao que pega a localizacao atual do cliente, caso este permita que sua localizacao seja utilizada,
    // plota um marcador no mapa e coloca foco do mapa nessa atual localizacao
    public void onLocationChanged(Location location) {

        //quando a localizacao atual do usuario mudar, ele retira o marcador anterior, caso tenha existido, e cria outro
        if (currentLocationMaker != null) {
            currentLocationMaker.remove();
        }
        //adiciona o marcador com a localizacao atual do usuario
        currentLocationLatLong = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(currentLocationLatLong);
        markerOptions.title("Localização atual");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
        currentLocationMaker = mMap.addMarker(markerOptions);
        currentLocationMaker.setDraggable(true);

        //quando a localizacao atual do usuario muda, o foco do mapa muda para o ponto atual do usuario e aumenta
        //o zoom do mapa, mostrando mais detalhes do mesmo
        CameraPosition cameraPosition = new CameraPosition.Builder().zoom(17).target(currentLocationLatLong).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        locationData.setLatitude(location.getLatitude());
        locationData.setLongitude(location.getLongitude());

        //Toast.makeText(this, "Localização atualizada", Toast.LENGTH_SHORT).show();
    }


    private ArrayList findUnAskedPermissions(ArrayList<String> wanted) {
        ArrayList result = new ArrayList();

        for (String perm : wanted) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (canAskPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }

    //caso a versao do celular seja maior que a marshmallow, o aplicativo pergunta se pode usar a localizacao
    //do usuario, caso contrario, ele ja tem essa informacao a partir das confifuracoes do arquivo do manifest
    private boolean canAskPermission() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    //funcao que verifica se o GPS do usuario esta ativado, e caso nao esteja, perguna ao mesmo se deseja ativa-lo
    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("GPS desativado!");
        alertDialog.setMessage("Ativar GPS?");
        alertDialog.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        alertDialog.setNegativeButton("Não", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        alertDialog.show();
    }

    //funcao que pega a localizaao atual do usuario, via GPS ou via Internet
    private void startGettingLocations() {

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        boolean isGPS = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetwork = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean canGetLocation = true;
        int ALL_PERMISSIONS_RESULT = 101;
        long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;// Distance in meters
        long MIN_TIME_BW_UPDATES = 1000 * 10;// Time in milliseconds

        ArrayList<String> permissions = new ArrayList<>();
        ArrayList<String> permissionsToRequest;

        permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        permissionsToRequest = findUnAskedPermissions(permissions);

        //Verifica se o GPS ou Internet do usuario esta ligada, caso nao esteja, pergunta se o mesmo deseja ativa-la
        if (!isGPS && !isNetwork) {
            showSettingsAlert();
        } else {

            //verifica permissoes de uso do GPS para versoes anteriores ao marshmallow
            //para versoes anteriores ao marshmallow, a localizacao ja pode ser adquirida a partir da
            //configucao do arquivo do manifest
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (permissionsToRequest.size() > 0) {
                    requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]),
                            ALL_PERMISSIONS_RESULT);
                    canGetLocation = false;
                }
            }
        }


        //verfica se a permissao para utilizar a localizacao foi concedida
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "Permissão negada", Toast.LENGTH_SHORT).show();
            return;
        }

        //atualiza a localizacao do usuario, a partir do GPS ou Intenet
        if (canGetLocation) {
            if (isGPS) {
                lm.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, (LocationListener) this);

            } else if (isNetwork) {
                lm.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, (LocationListener) this);

            }
        } else {
            Toast.makeText(this, "Não é possível obter a localização", Toast.LENGTH_SHORT).show();
        }
    }

    //funcoes padroes da classe LocationListerner. Nao foi necessario sobrescreve-las para manipular o mapa
    //e a localizacao atual do usuario
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public void nextPageButton(View view)
    {
        Location location;
        String nameShop = getIntent().getStringExtra("name");
        String infoShop = getIntent().getStringExtra("info");
        String referencePointShop = getIntent().getStringExtra("referencedPoint");

        if(this.locationData.getLatitude()!=0.0 && this.locationData.getLongitude()!=0.0) {

            Intent intent = new Intent(RegisterLocationActivity.this, RegisterFoodListActivity.class);
            intent.putExtra("name", nameShop);
            intent.putExtra("info", infoShop);
            intent.putExtra("referencedPoint", referencePointShop);
            intent.putExtra("latitude", locationData.getLatitude());
            intent.putExtra("longitude", locationData.getLongitude());
            intent.putExtra("picture", getIntent().getByteArrayExtra("picture"));

            startActivity(intent);

        }else {
            Toast.makeText(this, "Não foi possível pegar a sua localização", Toast.LENGTH_SHORT).show();
        }
    }
}