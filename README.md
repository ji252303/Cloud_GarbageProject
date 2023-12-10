## AWS를 이용한 IoT 클라우드 플랫폼 프로젝트

### 주제 : AWS를 이용한 세대 음식물 쓰레기통 관리 서비스

1. Arduino(MKR WIFI 1010)에 연결된 weight sensor을 이용해 쓰레기통의 무게를 감지해 DynamoDB에 값을 Upload

2. 업로드된 무게값이 일정 무게 이상일 경우 led가 점등하면서 경고 음식물 쓰레기를 수거해야한다고 알림

3. 업로드된 무게를 이용해 사람들이 평균적으로 버리는 음식물 쓰레기양보다 적은지 많은지 판단하는 알림과 효율적인 관리비 산출을 위해 대략적인 예산을 알수있도록 표시

4. 야간일경우 led 점등이 불편을 끼칠 수 있으므로 led를 끌 수 있도록 제어기능 탑재

![c](https://github.com/ji252303/Cloud_GarbageProject/issues/1#issue-2034315096)

## 1. Arduino MKR WIFI 1010 관련 Library 설치

- WIFININA
- ArduinoBearSSL
- ArduinoECCX08
- ArduinoMqttClient
- Arduino Cloud Provider Examples

## 2. ECCX08SCR예제를 통해 인증서 만들기

1. Arduino 파일 -> 예제 -> ArduinoECCX08 -> Tools -> ECCX08CSR

2. Serial Monitor를 연 후, Common Name: 부분에 garbageProject 입력(나머지 질문들은 입력 없이 전송 누르기) Would you like to generate? 에는 Y 입력

3. 생성된 CSR을 csr.txt 파일로 만들어 저장

## 3. AWS IoT Core에서 사물 등록하기

1. 관리 -> 사물 -> 단일 사물 생성 -> 사물 이름은 garbageProject 입력 -> CSR을 통한 생성을 Click -> 2번에서 저장한 csr.txt를 Upload -> 사물 등록

- region은 ap-southeast-2로 해줌./ 사물의 정책 AllowEverything(작업 필드 : iot.\* 관련) 생성 후 연결해줌.

2. 보안 -> 관리에서 생성된 인증서도 정책(AllowEverything)을 연결 해줌.

3. 생성된 인증서의 …를 Click한 후, 다운로드 선택

4. 다운로드 된 인증서 확인

## 4. Arduino_garbageProject/arduino_secrets.h

1. #define SECRET_SSID ""에 자신의 Wifi 이름을 적고, #define SECRET_PASS ""에 Wifi의 비밀번호를 적는다.

2. #define SECRET_BROKER "xxxxxxxxxxxxxx.iot.xx-xxxx-x.amazonaws.com"에는 설정에서 확인한 자신의 엔드포인트를 붙여넣기 한다.

3. const char SECRET_CERTIFICATE[] 부분에는, 3에서 다운 받은 인증서 긴 영어들을 복사 붙여넣기 해준다.

- 올바르게 작성 후, 업로드를 하면 Serial Monitor에는 network와 MQTT broker에 connect된 문구가 뜰것이다.

## 5. AWS DynamoDB 테이블 만들기 / Lambda함수 정의 / 규칙 정의

1.  테이블 만들기 -> 테이블 이름 : GarbageData / 파티션 키: deviceId(데이터 유형 : 문자열)

2.  정렬 키 추카 선택 -> time 입력(데이터 유형 : 숫자)

3.  Lambda함수 Eclipse용 AWS Toolkit 이용해 생성 & Upload

> Project name : RecordingDeviceDataJavaProject

> Group ID: com.example.lambda

> Artifact ID: recording

> >

```javascript
package helloworld;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class App implements RequestHandler<Document, String> {
    private DynamoDB dynamoDb;
    private String DYNAMODB_TABLE_NAME = "GarbageData";

    @Override
    public String handleRequest(Document input, Context context) {
        this.initDynamoDbClient();
        context.getLogger().log("Input: " + input);

        //return null;
        return persistData(input);
    }

    private String persistData(Document document) throws ConditionalCheckFailedException {

        // Epoch Conversion Code: https://www.epochconverter.com/
        SimpleDateFormat sdf = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        String timeString = sdf.format(new java.util.Date (document.timestamp*1000));

        return this.dynamoDb.getTable(DYNAMODB_TABLE_NAME)
                .putItem(new PutItemSpec().withItem(new Item().withPrimaryKey("deviceId", document.device)
                        .withLong("time", document.timestamp)
                        .withString("weight", document.current.state.reported.weight)
                        .withString("LED", document.current.state.reported.LED)
                        .withString("timestamp",timeString)))
                .toString();
    }

    private void initDynamoDbClient() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion("ap-southeast-2").build();

        this.dynamoDb = new DynamoDB(client);
    }

}

class Document {
    public Thing previous;
    public Thing current;
    public long timestamp;
    public String device;       // AWS IoT에 등록된 사물 이름
}

class Thing {
    public State state = new State();
    public long timestamp;
    public String clientToken;

    public class State {
        public Tag reported = new Tag();
        public Tag desired = new Tag();

        public class Tag {
            public String weight;

            public String LED;

        }
    }
}

```

> > 다음과 같이 수정 후, [Upload function to AWS Lambda] -> 함수 이름 :GarbageData -> dynamoDB정책에 연결되어있는 IAM 역할 선택 -> Upload

4.  AWS IoT Core -> 동작 -> 규칙 -> 이름 : garbageRule인 규칙 생성

> 규칙 쿼리 설명문 : SELECT \*, 'garbageProject' as device FROM '$aws/things/garbageProject/shadow/update/documents'

-> 작업 추가-> 메시지 데이터를 전달하는 Lambda 함수 호출 선택

> 5-3에서 upload한 GarbageData Lambda함수 선택

-> 작업 추가 -> 규칙 생성

## 6. API Gateway를 이용한 RestAPI 생성

### 0. CORS 활성화 및 API Gateway 콘솔에서 RESTAPI 배포 (공통)

> 0-1. 리소스 /devices 선택

> 0-2. 작업 드롭다운 메뉴 CORS 활성화(Enable CORS) 선택

> 0-3. CORS 활성화 및 기존의 CORS 헤더 대체 선택

> 0-4. 메서드 변경사항 확인 창에서 예, 기존 값을 대체하겠습니다. 선택

> 0-5. 작업 드롭다운 메뉴 API 배포 선택

> 0-6. 배포 스테이지 새스테이지 -> pro 생성

> 0-7. 배포

### 1. 디바이스 목록 조회 REST API 구축

> 1-1. Lambda 함수 생성

> > Project name : ListingDeviceLambdaJavaProject

> > > build.gradle에 의존성 추가

> > >

```javascript
<dependencies>
  ...
  <dependency>
    implementation platform('com.amazonaws:aws-java-sdk-bom:1.12.529')
    implementation 'com.amazonaws:aws-java-sdk-iot'
  </dependency>
</dependencies>
```

> > > ListingDeviceHandler.java Code

> > >

```javascript
package helloworld;
import java.util.List;
import com.amazonaws.services.iot.AWSIot;
import com.amazonaws.services.iot.AWSIotClientBuilder;
import com.amazonaws.services.iot.model.ListThingsRequest;
import com.amazonaws.services.iot.model.ListThingsResult;
import com.amazonaws.services.iot.model.ThingAttribute;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class App implements RequestHandler<Object, String> {

    @Override
    public String handleRequest(Object input, Context context) {

        // AWSIot 객체를 얻는다.
        AWSIot iot = AWSIotClientBuilder.standard().build();

        // ListThingsRequest 객체 설정.
        ListThingsRequest listThingsRequest = new ListThingsRequest();

        // listThings 메소드 호출하여 결과 얻음.
        ListThingsResult result = iot.listThings(listThingsRequest);

        return getResultStr(result);
    }


    /**
     * ListThingsResult 객체인 result로 부터 ThingName과 ThingArn을 얻어서 Json문자 형식의
     * 응답모델을 만들어 반환한다.
     * {
     * 	"things": [
     *	     {
     *			"thingName": "string",
     *	      	"thingArn": "string"
     *	     },
     *		 ...
     *	   ]
     * }
     */
    private String getResultStr(ListThingsResult result) {
        List<ThingAttribute> things = result.getThings();

        String resultString = "{ \"things\": [";
        for (int i =0; i<things.size(); i++) {
            if (i!=0)
                resultString +=",";
            resultString += String.format("{\"thingName\":\"%s\", \"thingArn\":\"%s\"}",
                    things.get(i).getThingName(),
                    things.get(i).getThingArn());

        }
        resultString += "]}";
        return resultString;
    }

}
```

> > > 다음과 같이 작성 후, [Upload function to AWS Lambda] -> 함수 이름 : ListThingsFunction -> AWSIoTFullAccess정책에 연결되어있는 IAM 역할 선택 -> Upload

> 1-2. API Gateway 콘솔에서 REST API 생성

> > 1.  API 생성

> > > API 유형 : REST API / API 이름 : garbage-api

> > 2.  리소스 아래 /를 선택 -> 작업 드롭다운 메뉴 리소스 생성을 선택 -> 리소스 이름 : devices 입력

> > 3.  작업 드롭다운 메뉴 메서드 생성(Create Method) 선택

> > 5.  리소스 이름 (/devices) 아래에 드롭다운 메뉴 -> GET을 선택 후 확인 표시 아이콘(체크) 선택

> > 6.  /devices – GET – 설정 -> 통합 유형에서 Lambda 함수를 선택 -> 저장

> > > - Lambda 프록시 통합 사용 상자를 선택하지 않은 상태 / Lambda 리전 : ap-southeast-2 / Lambda 함수 : ListThingsFunction

> > 7.  Lambda 함수에 대한 권한 추가 팝업(Lambda 함수를 호출하기 위해 API Gateway에 권한을 부여하려고 합니다....”) 확인 Click!

> > 8.  앞서 적은, 0. CORS 활성화 및 API Gateway 콘솔에서 RESTAPI 배포 실행!

> > > - 여기서는 작업 드롭다운 메뉴 -> API 배포 Click! -> 배포 스테이지 드롭다운 메뉴 [새 스테이지]를 선택 -> 스테이지 이름 : pro -> 배포

### 2. 디바이스 상태 조회 REST API 구축

> 2-1. Lambda 함수 생성

> > Project name : GetDeviceLambdaJavaProject

> > > build.gradle에 의존성 추가

> > >

```javascript
<dependencies>
  ...
  <dependency>
    implementation platform('com.amazonaws:aws-java-sdk-bom:1.12.529')
    implementation 'com.amazonaws:aws-java-sdk-iot'
  </dependency>
</dependencies>
```

> > > GetDeviceHandler.java Code

> > >

```javascript
package helloworld;

import com.amazonaws.services.iotdata.AWSIotData;
import com.amazonaws.services.iotdata.AWSIotDataClientBuilder;
import com.amazonaws.services.iotdata.model.GetThingShadowRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class App implements RequestHandler<Event, String> {

    @Override
    public String handleRequest(Event event, Context context) {
        AWSIotData iotData = AWSIotDataClientBuilder.standard().build();

        GetThingShadowRequest getThingShadowRequest  =
                new GetThingShadowRequest()
                        .withThingName(event.device);

        iotData.getThingShadow(getThingShadowRequest);

        return new String(iotData.getThingShadow(getThingShadowRequest).getPayload().array());
    }
}

class Event {
    public String device;
}

```

> > > 다음과 같이 수정 후, [Upload function to AWS Lambda] -> 함수 이름 : GetDeviceFunction -> AWSIoTFullAccess정책에 연결되어있는 IAM 역할 선택 -> Upload

> 2-2. API Gateway 콘솔에서 REST API 생성

> > 1.  생성한 garbage-api -> 리소스 이름(/devices)을 선택

> > 2.  작업 드롭다운 메뉴에서 리소스 생성을 선택 -> 리소스 이름 : device 입력 -> 리소스 경로(Resource Path)를 {device}로 바꾸기

> > 3.  API Gateway Cors 활성화 옵션을 선택 -> 리소스 생성

> > 4.  /{device} 리소스가 강조 표시되면 작업에서 메서드 생성(Create Method) 선택

> > 5.  리소스 이름 (/{devices}) 아래에 드롭다운 메뉴 -> GET을 선택 후 확인 표시 아이콘(체크) 선택

> > 6.  /devices/{device} – GET – 설정 -> 통합 유형에서 Lambda 함수를 선택 -> 저장

> > > - Lambda 프록시 통합 사용 상자를 선택하지 않은 상태 / Lambda 리전 : ap-southeast-2 / Lambda 함수 : GetDeviceFunction

> > 7.  Lambda 함수에 대한 권한 추가 팝업(Lambda 함수를 호출하기 위해 API Gateway에 권한을 부여하려고 합니다....”) 확인

> > 8.  /{device}의 GET 메서드의 통합 요청(Integration Request) 선택 -> 편집을 눌러 매핑 템플릿 -> 매핑 템플릿 추가

> > > - 요청 본문 패스스루 : 정의된 템플릿이 없는 경우(권장) / Content-Type : application/json

> > > - 템플릿 생성 밑에 다음 code 작성 -> 저장

```javascript
{
  "device": "$input.params('device')"
}
```

### 3. 디바이스 상태 업데이트 REST API 구축

> 3-1. Lambda 함수 생성

> > Project name : updateDeviceLambdaJavaProject

> > > build.gradle에 의존성 추가

> > >

```javascript
<dependencies>
  ...
  <dependency>
    implementation platform('com.amazonaws:aws-java-sdk-bom:1.12.529')
    implementation 'com.amazonaws:aws-java-sdk-iot'
  </dependency>
</dependencies>
```

> > > updateDeviceLambda.java Code

> > >

```javascript
package helloworld;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.iotdata.AWSIotData;
import com.amazonaws.services.iotdata.AWSIotDataClientBuilder;
import com.amazonaws.services.iotdata.model.UpdateThingShadowRequest;
import com.amazonaws.services.iotdata.model.UpdateThingShadowResult;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<Event, String> {

    public String handleRequest(final Event event, final Context context) {
        AWSIotData iotData = AWSIotDataClientBuilder.standard().build();

        String payload = getPayload(event.tags);

        UpdateThingShadowRequest updateThingShadowRequest  =
                new UpdateThingShadowRequest()
                        .withThingName(event.device)
                        .withPayload(ByteBuffer.wrap(payload.getBytes()));

        UpdateThingShadowResult result = iotData.updateThingShadow(updateThingShadowRequest);
        byte[] bytes = new byte[result.getPayload().remaining()];
        result.getPayload().get(bytes);
        String output = new String(bytes);

        return output;
    }

    private String getPayload(ArrayList<Tag> tags) {
        String tagstr = "";
        for (int i=0; i < tags.size(); i++) {
            if (i !=  0) tagstr += ", ";
            tagstr += String.format("\"%s\" : \"%s\"", tags.get(i).tagName, tags.get(i).tagValue);
        }
        return String.format("{ \"state\": { \"desired\": { %s } } }", tagstr);
    }
}

class Event {
    public String device;
    public ArrayList<Tag> tags;

    public Event() {
        tags = new ArrayList<Tag>();
    }
}

class Tag {
    public String tagName;
    public String tagValue;

    @JsonCreator
    public Tag() {
    }

    public Tag(String n, String v) {
        tagName = n;
        tagValue = v;
    }
}
```

> > > 다음과 같이 작성 후, [Upload function to AWS Lambda] -> 함수 이름 : UpdateDeviceFunction -> AWSIoTFullAccess정책에 연결되어있는 IAM 역할 선택 -> Upload

> 2-2. API Gateway 콘솔에서 REST API 생성

> > 1.  생성한 garbage-api -> 리소스 이름(/devices)을 선택

> > 2.  /{device} 리소스가 강조 표시되면 작업에서 메서드 생성(Create Method) 선택

> > 3.  리소스 이름 (/{devices}) 아래에 드롭다운 메뉴 -> PUT을 선택 후 확인 표시 아이콘(체크) 선택

> > 4.  /devices/{device} – PUT – 설정 -> 통합 유형에서 Lambda 함수를 선택 -> 저장
> >     Lambda 프록시 통합 사용 상자를 선택하지 않은 상태 / Lambda 리전 : ap-southeast-2 / Lambda 함수 : UpdateDeviceFunction

> > 5.  Lambda 함수에 대한 권한 추가 팝업(Lambda 함수를 호출하기 위해 API Gateway에 권한을 부여하려고 합니다....”) 확인

> > 6.  모델 Click(리소스, 스테이지 등 메뉴가 있는 곳) -> 생성 Click! -> 작성 후, 모델 생성 Click!
> >     모델 이름 : UpdateDeviceInput / 콘텐츠 유형 : application/json
> >     모델 스키마 다음 code 작성!

> > > - 요청 본문 패스스루 : 정의된 템플릿이 없는 경우(권장) / Content-Type : application/json

> > > - 템플릿 생성 밑에 다음 code 작성 -> 저장

```
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "title": "UpdateDeviceInput",
  "type" : "object",
  "properties" : {
      "tags" : {
          "type": "array",
          "items": {
              "type": "object",
              "properties" : {
                "tagName" : { "type" : "string"},
                "tagValue" : { "type" : "string"}
              }
          }
      }
  }
}
```

> > > 추가 팝업 예, 이 통합 보호(Yes, secure this integration) Click!
> > > 템플릿 생성 UpdateDeviceInput 선택 -> 매핑 탬플릿 편집기에 다음과 같은 code 작성 -> 저장

```
#set($inputRoot = $input.path('$'))
{
    "device": "$input.params('device')",
    "tags" : [
    ##TODO: Update this foreach loop to reference array from input json
        #foreach($elem in $inputRoot.tags)
        {
            "tagName" : "$elem.tagName",
            "tagValue" : "$elem.tagValue"
        }
        #if($foreach.hasNext),#end
        #end
    ]
}
```

### 4. 디바이스 로그 조회 REST API 구축

> 4-1. Lambda 함수 생성

> > Project name : LogDeviceLambdaJavaProject

> > 위와 같이 의존성 추가를 해줘야 한다.

> > > LogDeviceHandler.java Code

> > >

```javascript
package helloworld;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.TimeZone;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class App implements RequestHandler<Event, String> {

    private DynamoDB dynamoDb;
    private String DYNAMODB_TABLE_NAME = "GarbageData";

    @Override
    public String handleRequest(Event input, Context context) {
        this.initDynamoDbClient();

        Table table = dynamoDb.getTable(DYNAMODB_TABLE_NAME);

        long from=0;
        long to=0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

            from = sdf.parse(input.from).getTime() / 1000;
            to = sdf.parse(input.to).getTime() / 1000;
        } catch (ParseException e1) {
            e1.printStackTrace();
        }

        QuerySpec querySpec = new QuerySpec()
                .withKeyConditionExpression("deviceId = :v_id and #t between :from and :to")
                .withNameMap(new NameMap().with("#t", "time"))
                .withValueMap(new ValueMap().withString(":v_id",input.device).withNumber(":from", from).withNumber(":to", to));

        ItemCollection<QueryOutcome> items=null;
        try {
            items = table.query(querySpec);
        }
        catch (Exception e) {
            System.err.println("Unable to scan the table:");
            System.err.println(e.getMessage());
        }

        return getResponse(items);
    }

    private String getResponse(ItemCollection<QueryOutcome> items) {

        Iterator<Item> iter = items.iterator();
        String response = "{ \"data\": [";
        for (int i =0; iter.hasNext(); i++) {
            if (i!=0)
                response +=",";
            response += iter.next().toJSON();
        }
        response += "]}";
        return response;
    }

    private void initDynamoDbClient() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();

        this.dynamoDb = new DynamoDB(client);
    }
}

class Event {
    public String device;
    public String from;
    public String to;
}
```

> > > 다음과 같이 작성 후, [Upload function to AWS Lambda] -> 함수 이름 : LogDeviceFunction -> AmazonDynamoDBFullAccess정책에 연결되어있는 IAM 역할 선택 -> Upload

> 4-2. API Gateway 콘솔에서 REST API 생성

> > 1.  생성한 garbage-api Click! -> 리소스 이름(/{device})을 선택

> > 2.  작업 드롭다운 메뉴에서 리소스 생성을 선택 -> 리소스 이름 : log 입력 -> /log – GET – 설정

> > 3.  /devices/{device}/log – GET – 설정 -> 통합 유형에서 Lambda 함수를 선택 -> 저장

> > > - Lambda 프록시 통합 사용 상자를 선택하지 않은 상태 / Lambda 리전 : ap-southeast-2 / Lambda 함수 : LogDeviceFunction

> > 4.  Lambda 함수에 대한 권한 추가 팝업(Lambda 함수를 호출하기 위해 API Gateway에 권한을 부여하려고 합니다....”) 확인

> > 5.  메서드 요청 Click -> URL 쿼리 문자열 파라미터(URL Query String Parameters)

> > > 쿼리 문자열 추가 : from (캐싱 uncheck) / 쿼리 문자열 추가(Add query string) : to (캐싱 uncheck)

> > 6.  /log GET 메서드 메서드의 통합 요청(Integration Request) 선택 -> 매핑 템플릿 -> 매핑 템플릿 추가

> > > - 요청 본문 패스스루 : 정의된 템플릿이 없는 경우(권장) / Content-Type : application/json

> > 7.  추가 팝업 예, 이 통합 보호(Yes, secure this integration)

> > > - 템플릿 생성 UpdateDeviceInput 선택 -> 매핑 탬플릿 편집기에 다음과 같은 code 작성 -> 저장

```javascript
{
  "device": "$input.params('device')",
  "from": "$input.params('from')",
  "to":  "$input.params('to')"
}
```

> > 8.  앞서 적은, 0. CORS 활성화 및 API Gateway 콘솔에서 RESTAPI 배포 실행

## File 설명

### 1. arduino_hx711

- Weight Sensor관련 library

#### 2. arduino_hx711.ino

> 1. Weight sensor 관련 library, Led, mkr, wifi 관련 파일들 정의

- Arduino에 연결된 Weight Sensor, Led의 현재 상태를 topic에 update

#### 2-1 Led.h, Led.cpp

> 1. Led.h, Led.cpp : Led관련 입출력 PIN 설정, ON/OFF기능 구현, Led의 ON/OFF 상태 관련

> > (LED가 ON이면 led가 켜지도록 코드 작성)

### 3. android_garbageproject/GarbageProject(Android App Code)

- RealActivity, AllActivity에 있는 urlStr1, urlStr2는 각각 6. API Gateway를 이용한 RestAPI 생성에서 만든 API 서버 URL

> urlStr1 : 디바이스 상태 조회/변경 API URL(https://xxxxxxxx.execute-api.ap-southeast-2.amazonaws.com/prod/devices/{devices_name})

> urlStr2 : 디바이스 로그 조회 API URL(https://xxxxxxxx.execute-api.ap-southeast-2.amazonaws.com/prod/devices/{devices_name}/log)
