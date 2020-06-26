package wonyong.by.zolzolzol

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_information.*

class InformationActivity : AppCompatActivity() {
    lateinit var rdb : DatabaseReference
    var toiletName = ""
    var openTime = ""
    var comment = ""
    var phoneNumber = ""
    var editMode = false//true -> edit 일때, false ->textView일때
    var both = ""
    var nowlatitude: Double = 0.0
    var nowlongtitude: Double = 0.0
    var toiletLatitude: Double = 0.0
    var toiletLongtitude: Double = 0.0
    var distance = 0
    var distanceKM = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_information)
        rdb = FirebaseDatabase.getInstance().getReference("Toilet/items")
        init()
        buttonInit()
    }

    private fun buttonInit() {
        EDIT_BUTTON.setOnClickListener{
            if(editMode) {
                var newComment = EDIT_COMMENT.text.toString()
                rdb.child(toiletName).child("COMMENT").setValue(newComment)
                EDIT_COMMENT.visibility = View.GONE
                COMMENT.visibility = View.VISIBLE
                COMMENT.text = newComment
                if(newComment.equals("")){
                    COMMENT.text = "작성된 상태 정보가 없습니다.\n 사용에 문제가 있으면 버튼을 눌러 작성해주세요!"
                }
                EDIT_BUTTON.text = "상태변경"
                editMode = false
            }else{
                COMMENT.visibility = View.GONE
                EDIT_COMMENT.visibility = View.VISIBLE
                EDIT_BUTTON.text = "작성완료"
                editMode = true
            }
        }
        CALL_BUTTON.setOnClickListener{
            if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE)
            ==PackageManager.PERMISSION_GRANTED){
                val i = Intent(Intent.ACTION_CALL, Uri.parse("tel:"+phoneNumber))
                startActivity(i)
            }else{
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CALL_PHONE), 100)
            }

        }
    }

    fun init(){
        toiletName = intent.getStringExtra("toiletName")
        openTime = intent.getStringExtra("openTime")
        phoneNumber = intent.getStringExtra("phoneNumber")
        both = intent.getStringExtra("both")
        toiletLatitude = intent.getDoubleExtra("toiletLatitude", 0.0)
        toiletLongtitude = intent.getDoubleExtra("toiletLongtitude", 0.0)
        TOILET_NAME.text = "화장실 이름 : $toiletName"
        EDIT_COMMENT.visibility = View.GONE
        nowlatitude = intent.getDoubleExtra("lat", 0.0)
        nowlongtitude = intent.getDoubleExtra("logt", 0.0)

        rdb.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                comment = dataSnapshot.child(toiletName).child("COMMENT").getValue().toString()
                OPEN_TIME.text = "개방시간 : $openTime"
                COMMENT.text = comment
                if(comment.equals("")){
                    COMMENT.text = "작성된 상태 정보가 없습니다.\n 사용에 문제가 있으면 버튼을 눌러 작성해주세요!"
                }
                if(phoneNumber == ""){
                    MANAGE_PHONE.text = "전화없음"
                    CALL_BUTTON.visibility = View.GONE
                }else {
                    MANAGE_PHONE.text = "관리자 전화번호 : $phoneNumber"
                }

                if(both.equals("Y")){
                    imageView.setImageResource(R.drawable.both)
                }else{
                    imageView.setImageResource(R.drawable.nonboth)
                }

                distance = CalculateDistance().distance(nowlatitude, nowlongtitude, toiletLatitude, toiletLongtitude).toInt()
                if(distance >= 1000){
                    distanceKM = Math.round((distance / 1000.0)*100)/100.0
                    DISTANCE.text = distanceKM.toString() +
                            "km 거리에 있습니다."
                }else {
                    DISTANCE.text = distance.toString() +
                            "m 거리에 있습니다."
                }

            }

            override fun onCancelled(p0: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }



    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 100){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                val i = Intent(Intent.ACTION_CALL, Uri.parse("tel:"+phoneNumber))
                startActivity(i)
            }else{
                Toast.makeText(this, "전화걸기 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}