package thirdweek.madcamp.walkitalki;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.firebase.messaging.RemoteMessage;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.MeV2ResponseCallback;
import com.kakao.usermgmt.response.MeV2Response;

import net.daum.mf.map.api.CalloutBalloonAdapter;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import thirdweek.madcamp.walkitalki.Model.Chat;
import thirdweek.madcamp.walkitalki.Model.ChatVer2;
import thirdweek.madcamp.walkitalki.Model.Post;
import thirdweek.madcamp.walkitalki.Model.User;
import thirdweek.madcamp.walkitalki.Retrofit.APIUtils;
import thirdweek.madcamp.walkitalki.Retrofit.IMyService;

import static android.content.Context.NOTIFICATION_SERVICE;

public class Fragment1 extends Fragment {

    private LocationManager locationManager;
    private LocationListener locationListener;

    private Socket socket;
    public EditText messagetxt2;
    public Button sendBtn;
    public Button postBtn;
    public String Name;

    public static String KAKAONAME;
    public static long KAKAOID;

    public static User myUser;

    private static double longitude;
    private static double latitude;
    private MapView mMapView;
    private MapPOIItem mCustomMarker;


    private final String CHANNEL_ID = "walkitalki_notifications";
    private final int NOTIFICATION_ID = 001;

    public Fragment1() {
        //Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            socket = IO.socket("http://socrip4.kaist.ac.kr:1380/");
            socket.connect();
            this.requestMe();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //현재 위치 정보 받는 기능
        locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //위치 바뀔때마다 latitude, longitude  바뀜
                Log.d("LocationL: ", location.toString());
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(getContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        final PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);



        //위치 정보 권한 체크 (없으면 권한 받기)
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return null;
        }



        //위치 업데이트 물어보기 (0초마다, 0만큼 움직였을때)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);


        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_layout1, container, false);


        //지도 띄우기
        final MapView mapView = new MapView(getActivity());
        ViewGroup mapViewContainer = (ViewGroup) v.findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);



        //Fragment에 있는 메시지 기능 item들
        messagetxt2 = (EditText) v.findViewById(R.id.message2);
        sendBtn = (Button) v.findViewById(R.id.send2);



        //이미 있는 쳇들 찍기
        final IMyService iMyService;
        iMyService = APIUtils.getUserService();
        Call call = iMyService.getChats();
        call.enqueue(new Callback<List<Chat>>() {
            @Override
            public void onResponse(Call<List<Chat>> call, Response<List<Chat>> response) {
                if(response.body() != null) {
                   List<Chat> tempList =  response.body();
                    final MyUtil myUtil = new MyUtil(getContext());
                    for (int i = 0 ; i<tempList.size(); i++){

                        Chat tmpChat = new Chat();
                        tmpChat.username = tempList.get(i).username;
                        tmpChat.content = tempList.get(i).content;
                        tmpChat.latitude = tempList.get(i).latitude;
                        tmpChat.longitude = tempList.get(i).longitude;
                        tmpChat.userID = tempList.get(i).userID;
                        Log.e(" "+i+"번째 유저 정보는 ", tmpChat.username + tmpChat.content + tmpChat.latitude +  tmpChat.longitude );

                        final Chat tmpChatMSG = new Chat(tmpChat.username, tmpChat.userID, tmpChat.content, tmpChat.latitude, tmpChat.longitude);


                        myUtil.popOthersMsg(mapView, tmpChatMSG, tmpChat.latitude, tmpChat.longitude );
                    }
                }
            }
            @Override
            public void onFailure(Call<List<Chat>> call, Throwable t) {
            }
        });


        //이미 있는 포스트들 찍기
        Call call2 = iMyService.getPosts();
        call2.enqueue(new Callback<List<Post>>() {
            @Override
            public void onResponse(Call<List<Post>> call, Response<List<Post>> response) {
                if(response.body() != null) {
                    List<Post> tempList =  response.body();
                    final MyUtil myUtil = new MyUtil(getContext());
                    for (int i = 0 ; i<tempList.size(); i++){

                        Post tmpChat = new Post();
                        tmpChat.title = tempList.get(i).title;
                        tmpChat.username = tempList.get(i).username;
                        tmpChat.content = tempList.get(i).content;
                        tmpChat.latitude = tempList.get(i).latitude;
                        tmpChat.longitude = tempList.get(i).longitude;
                        tmpChat.userID = tempList.get(i).userID;
                        Log.e(" "+i+"번째 유저 정보는 ", tmpChat.username + tmpChat.content + tmpChat.latitude +  tmpChat.longitude );

                        final Post tmpChatMSG = new Post(tmpChat.username, tmpChat.userID,tmpChat.title, tmpChat.content, tmpChat.latitude, tmpChat.longitude);
//                        mapView.setCalloutBalloonAdapter(new CustomCalloutBalloonAdapter());
                        myUtil.popOthersPost(mapView, tmpChatMSG, tmpChat.latitude, tmpChat.longitude);
                    }
                }
            }

            @Override
            public void onFailure(Call call, Throwable t) {

            }
        });



        //실시간 메시지 맵에 찍기
        socket.on("map new message", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("newMSG", "NEW MSG!");
                        JSONObject data = (JSONObject) args[0];
                        try {
                            //실시간 데이터 정리
                            String name = data.getString("username");
                            Long userID = data.getLong("userID");
                            String message = data.getString("message");
                            double msgLatitude = data.getDouble("latitude");
                            double msgLongitude = data.getDouble("longitude");

                            final MyUtil myUtil = new MyUtil(getContext());
                            Chat tmpChat = new Chat(name, userID, message, msgLatitude, msgLongitude);

                            myUtil.popOthersMsg(mapView, tmpChat, msgLatitude, msgLongitude);

                            createNotificationChannel();
                            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getContext(), CHANNEL_ID)
                                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                                    .setContentTitle("WalkiTalki")
                                    .setContentText("새로운 메세지가 있습니다")
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    .setContentIntent(pendingIntent);

                            NotificationManagerCompat notificationCompat = NotificationManagerCompat.from(getContext());
                            notificationCompat.notify(NOTIFICATION_ID, mBuilder.build());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });


        //실시간 포스트 찍기
        socket.on("map new post", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("newPOST", "NEW POST");
                        JSONObject data = (JSONObject) args[0];
                        try {
                            //실시간 데이터 정리
                            String title = data.getString("title");
                            String name = data.getString("username");
                            Long userID = data.getLong("userID");
                            String message = data.getString("message");
                            double msgLatitude = data.getDouble("latitude");
                            double msgLongitude = data.getDouble("longitude");

                            final MyUtil myUtil = new MyUtil(getContext());
                            Post tmpPost = new Post(name, userID,title, message, msgLatitude, msgLongitude);

                            Log.e("BEFORE", "popotherspost");
                            myUtil.popOthersPost(mapView,tmpPost , msgLatitude, msgLongitude);
                            Log.e("AFTER", "popotherspost");

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });




        // 줌 레벨 변경, 모드 변경
        mapView.setZoomLevel(4, true);
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving);



        //중심점 내 위치로 변경
        FloatingActionButton button_mylocation = v.findViewById(R.id.button_mylocation);
        button_mylocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(latitude,longitude), true);
            }
        });



        //메시지 서버에 보내기 (실시간으로 다시 디바이스로 전송 + 데이터 베이스에 저장됨)
        sendBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(!messagetxt2.getText().toString().isEmpty()){
                    socket.emit("map detection", KAKAONAME, KAKAOID, messagetxt2.getText().toString(), latitude, longitude);
                    messagetxt2.setText("");
                }
            }
        });

        postBtn = v.findViewById(R.id.post_button);
        postBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), UploadPostActivity.class);
                startActivity(intent);
            }
        });


        //20초 뒤에 모든 핀(메세지 + 포스트)들 지우기
        TimerTask checkMap = new TimerTask() {
            @Override
            public void run() {
                if (mapView != null ) {
                    TimerTask tt = new TimerTask() {
                        @Override
                        public void run() {
                            mapView.removeAllPOIItems();

                            //데이터에있는 메시지 다시 찍기
                            Call call = iMyService.getChats();
                            call.enqueue(new Callback<List<Chat>>() {
                                @Override
                                public void onResponse(Call<List<Chat>> call, Response<List<Chat>> response) {
                                    if(response.body() != null) {
                                        List<Chat> tempList =  response.body();
                                        final MyUtil myUtil = new MyUtil(getContext());
                                        for (int i = 0 ; i<tempList.size(); i++){

                                            Chat tmpChat = new Chat();
                                            tmpChat.username = tempList.get(i).username;
                                            tmpChat.content = tempList.get(i).content;
                                            tmpChat.latitude = tempList.get(i).latitude;
                                            tmpChat.longitude = tempList.get(i).longitude;
                                            tmpChat.userID = tempList.get(i).userID;
                                            Log.e(" "+i+"번째 유저 정보는 ", tmpChat.username + tmpChat.content + tmpChat.latitude +  tmpChat.longitude );

                                            final Chat tmpChatMSG = new Chat(tmpChat.username, tmpChat.userID, tmpChat.content, tmpChat.latitude, tmpChat.longitude);


                                            myUtil.popOthersMsg(mapView, tmpChatMSG, tmpChat.latitude, tmpChat.longitude );
                                        }
                                    }
                                }
                                @Override
                                public void onFailure(Call<List<Chat>> call, Throwable t) {
                                }
                            });

                            //데이터에있는 포스트 다시 찍기
                            Call call2 = iMyService.getPosts();
                            call2.enqueue(new Callback<List<Post>>() {
                                @Override
                                public void onResponse(Call<List<Post>> call, Response<List<Post>> response) {
                                    if(response.body() != null) {
                                        List<Post> tempList =  response.body();
                                        final MyUtil myUtil = new MyUtil(getContext());
                                        for (int i = 0 ; i<tempList.size(); i++){

                                            Post tmpChat = new Post();
                                            tmpChat.title = tempList.get(i).title;
                                            tmpChat.username = tempList.get(i).username;
                                            tmpChat.content = tempList.get(i).content;
                                            tmpChat.latitude = tempList.get(i).latitude;
                                            tmpChat.longitude = tempList.get(i).longitude;
                                            tmpChat.userID = tempList.get(i).userID;
                                            Log.e(" "+i+"번째 유저 정보는 ", tmpChat.username + tmpChat.content + tmpChat.latitude +  tmpChat.longitude );

                                            final Post tmpChatMSG = new Post(tmpChat.username, tmpChat.userID,tmpChat.title, tmpChat.content, tmpChat.latitude, tmpChat.longitude);
                                            myUtil.popOthersPost(mapView, tmpChatMSG, tmpChat.latitude, tmpChat.longitude);
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call call, Throwable t) {

                                }
                            });
                        }
                    };
                    Timer timer = new Timer();
                    timer.schedule(tt, 0, 20000);
                }
            }
        };
        Timer timer = new Timer();
        timer.schedule(checkMap, 0, 20000);




        return v;
    }


    @Override
    public void onResume() {
        super.onResume();
        try {
            socket = IO.socket("http://socrip4.kaist.ac.kr:1380/");
            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        socket.disconnect();
    }


    //유저의 정보를 받아오는 함수
    protected void requestMe() {

        UserManagement.getInstance().me(new MeV2ResponseCallback() {
            @Override
            public void onFailure(ErrorResult errorResult) {
            }

            @Override
            public void onSessionClosed(ErrorResult errorResult) {
            }

            @Override
            public void onSuccess(MeV2Response result) {
                KAKAOID = result.getId();
                KAKAONAME = result.getNickname();

                //TODO: 카카오톡 프로필 사진 받아오기

                Log.d("KAKAOID", String.valueOf(KAKAOID));
                Log.d("KAKAOAME", KAKAONAME);

                myUser = new User(KAKAONAME, KAKAOID);
            }
        });
    }


    //Permission 확인
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(getActivity().NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
    }

}
