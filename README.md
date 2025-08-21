# Filter Sample App


## 필터 적용 방식

본 프로젝트에서는 **Android View**와 **Jetpack Compose** 두 가지 환경 모두에서 동일한 필터 로직을 구현하였으며, 기능 구현에 집중하여 작성되었습니다.

### 1. 전체 구조

- **구현 방식**
  - `View`, `ViewModel`만 구현하여 역할을 분리함
  - `View`는 **UI를 보여주는 역할**, `ViewModel`은 **상태를 저장하고 업데이트하는 역할**을 담당
- **상태 관리**
  - 화면에 존재하는 상태를 단일 상태(`MainUiState, MainComposeUiState`)로 정의
  - 상태 변경 시 `copy()`를 통해 **불변 객체**로 업데이트
- **흐름**
  1. 사용자가 버튼을 토글  
  2. `ViewModel`에서 Update 과정을 거쳐 상태 업데이트  
  3. `View`에서 상태를 옵저빙하여 UI에 반영  

```kotlin
data class MainUiState(
    val isGray: Boolean,
    val isBright: Boolean,
    val isRestored: Boolean,
    val grayScaleColorMatrix: ColorMatrix,
    val brightnessColorMatrix: ColorMatrix,
    val colorFilter: ColorFilter,
    val cachedIsGray: Boolean,
    val cachedIsBright: Boolean,
    val cachedColorFilter: ColorFilter
)
```

### 2. 필터 구현 방식
- 흑백(ColorMatrix)과 밝기(ColorMatrix)를 `concat`하여 최종 `ColorMatrix`를 생성
- 생성된 `ColorMatrix`를 `ColorFilter`로 변형 후 `ImageView` 또는 `Compose Image`에 적용


### 3. 흑백 모드 전환
- `saturation`(채도) 값을 기반으로 **흑백 행렬** 생성
  - `saturation = 0f` → 모든 색상을 **회색조(Grayscale)** 로 변환
  - 변환 시 RGB 행에는 고정 계수(`0.213, 0.715, 0.072`)가 적용됨
  

### 4. 밝기 모드 전환
- **Offset, Scale 값 변경**을 통해 밝기 변환 행렬 생성
  - 밝기 전용 `ColorMatrix`의 Offset, Scale RGB 값을 변경하여 밝기 조절
  - 어두운 영역은 더 어둡게, 밝은 영역은 더 밝게 되면서 대비를 확대

### 4. 되돌리기 (이미지 비교만을 위한 목적)
- **되돌리기**
  - 이전 이미지 상태로 복원
  - 흑백/밝기 버튼 모두 비활성화
  - 현재 상태(`isGray`, `isBright`, `colorFilter`)를  
    `cachedIsGray`, `cachedIsBright`, `cachedColorFilter`에 저장 후 현재 상태는 초기 상태로 리셋
- **복원하기**
  - 캐시에 저장된 상태를 다시 현재 상태로 복원
  - 흑백/밝기 버튼 모두 활성화
  - `cachedIsGray`, `cachedIsBright`, `cachedColorFilter`는 초기 상태로 리셋

---

## 구현 중 겪은 문제와 질문 사항

### 1. 구현 중 겪은 문제

- **⚠️ `ColorMatrix` 불변 객체 혼동**
  - Compose 환경에서 기존 `ColorMatrix` 객체를 직접 변경하면, 동일 객체로 인식되어 리컴피조션이 발생하지 않는 문제가 있었음.
  - 해결: stable한 `ColorFilter` 인스턴스를 생성하여 상태를 갱신하도록 구현.

- **⚠️ `setToSaturation` 사용 시 reset 문제**
  - `setToSaturation`은 내부적으로 `reset()`을 호출하므로, 누적된 `ColorMatrix`에 적용시 적용 값들이 초기화됨.
  - 해결: 흑백 변경 전용 `ColorMatrix`를 별도로 생성하고, 기존 밝기 행렬과 `concat`하여 최종 `ColorMatrix` 변경.

- **⚠️ `ColorMatrix` 적용 방식에 따른 결과 차이**
  - `ColorMatrix`를 여러 개 합성(`postConcat)`해서 사용하는 경우와, 각각의 `ColorMatrix` 따로 `ColorMatrixColorFilter`로 적용하는 경우 결과가 달라질 수 있음 (아래 예시)

---

```kotlin
val cm = ColorMatrix(
    floatArrayOf(
        1f, 0f, 0f, 0f, -255f,   // R - 255
        0f, 1f, 0f, 0f, -255f,   // G - 255
        0f, 0f, 1f, 0f, -255f,   // B - 255
        0f, 0f, 0f, 1f,   0f     // A 그대로
    )
)

cm.postConcat(
    ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 255f,   // R + 255
            0f, 1f, 0f, 0f, 255f,   // G + 255
            0f, 0f, 1f, 0f, 255f,   // B + 255
            0f, 0f, 0f, 1f,   0f    // A 그대로
        )
    )
)

imageView.colorFilter = ColorMatrixColorFilter(cm)
```

```kotlin
val cm1 = ColorMatrix(
    floatArrayOf(
        1f, 0f, 0f, 0f, -255f,
        0f, 1f, 0f, 0f, -255f,
        0f, 0f, 1f, 0f, -255f,
        0f, 0f, 0f, 1f,   0f
    )
)

imageView.colorFilter = ColorMatrixColorFilter(cm1)

val cm2 = ColorMatrix(
    floatArrayOf(
        1f, 0f, 0f, 0f, -255f,
        0f, 1f, 0f, 0f, -255f,
        0f, 0f, 1f, 0f, -255f,
        0f, 0f, 0f, 1f,   0f
    )
)
imageView.colorFilter = ColorMatrixColorFilter(cm2)
```

  - 해결: 개별 필터를 Image에 순차 적용하는 대신, 모든 변환 행렬을 미리 `concat`하여 한 번에 적용

### 2. 질문 사항

- **프리셋 선정 기준**
  - 다양한 필터 프리셋이 존재하는데, 각 필터가 주는 느낌(특히 레트로 감성)을 선정하고 R&D하는 기준이 궁금합니다.
  
- **프리셋 추천 기준**
  - 사용자에게 필터 프리셋을 추천할 때, 어떤 요소(색감, 대비, 채도, 특정 톤 등)를 기준으로 추천하는지 궁금합니다.
  - 추천 알고리즘은 어떻게 구현되었는지도 궁금합니다.

- **최소 버전 설정 이유**
  - 요구사항에서는 최소 버전을 21이상으로 설정했는데 그 이유가 궁금합니다.
  - 조사를 해보니 21 버전 업데이트 문서에서 OpenGL 관련 업데이트는 있지만, 필터 관련 업데이트는 찾을 수 없기에 그 이유가 궁금합니다.

---

## 필터 적용에 대한 생각

### 1. 구현 과정에서 느낀 점
  - 필터 옵션은 종류가 다양하고, 옵션 간 조합이 많아질수록 테스트와 고려해야 할 요소가 급격히 늘어나기 떄문에 테스트 코드에 대한 필요성이 높을 것 같습니다.

### 2. 사용자 가치
  - 필터를 통해 표현할 수 있는 이미지의 종류는 무궁무진하며, 이를 통해 사용자에게 다양한 시각적 경험을 제공할 수 있다고 생각합니다.
  - 사진은 보통 기억하고 싶은 순간이나 좋아하는 사람들과 함께한 장면을 담는 경우가 많은데, 다양한 필터를 제공함으로써 그 순간이 더욱 특별하고 아름답게 기억되도록 도울 수 있다고 생각합니다.
  - 과거에는 다양한 분위기의 사진을 촬영하려면 고가의 카메라 장비가 필요했지만, 이제는 스마트폰과 간단한 필터 기능만으로도 손쉽게 원하는 느낌을 연출할 수 있어, **누구나 접근 가능한 사진 경험**을 제공할 수 있다고 생각합니다.

---

## 코드 확장에 대한 생각  
*(수백 개의 필터를 서버를 통해 다운로드받는 앱으로 확장할 경우 고려사항)*

1. **대량 다운로드 시간 및 정책**
   - 수백 개의 필터를 한 번에 선행 다운로드하려면 초기 로딩 시간이 길어질 수 있습니다. (아래 이미지가 필터를 선행 다운로드 받는 과정이라고 생각합니다)

     <img width="240" height="540" alt="image" src="https://github.com/user-attachments/assets/b5cc8c3b-5596-4944-a7fe-a6855a1177e5" />

   - 다운로드 완료 전까지 앱 접근을 제한할지, 혹은 기본 필터와 이미 다운로드된 필터만 사용 가능하게 할지 정책을 결정해야 합니다.
   


3. **네트워크 오류 및 오프라인 대응**
   - 다운로드 중 네트워크 오류나 오프라인 상태가 발생하면 앱이 정상 동작하지 않을 수 있습니다.
   - 현재까지 다운로드된 필터와 기본 필터만 사용하도록 예외 처리를 해야 할 수도 있습니다.

4. **백그라운드/포그라운드 다운로드**
   - 다운로드 도중 앱을 백그라운드로 전환 시에도 현재 다운로드 상태를 보여주기 위해  **Foreground Service** 를 활용해야 합니다.
   - 신규 필터를 다운로드 받기 위해서 주기적으로 **WorkManager**를 활용하는 것도 방법이 될 수 있다고 생각합니다

5. **부분 다운로드 및 캐싱**
   - 다운로드 중 실패한 항목은 캐시에 저장하여, 재시도 시 변경된 필터나 누락된 필터만 업데이트하도록 구현해야 합니다.

6. **데이터 크기와 스트리밍**
   - 필터 데이터가 대용량일 경우, 한 번에 로드하지 않고 **스트리밍 방식** 또는 청크 단위로 나눠서 다운로드해야 합니다.
   - 프리셋당 꼭 필요한 정보만 전송하도록 서버-클라이언트 간 데이터 구조를 최적화해야 합니다.

7. **저장소 구조 및 DB 설계**
   - 프리셋 데이터를 로컬 DB(Room)에 저장할 경우, 대용량 컬럼은 BLOB 타입으로 저장해야 합니다.
   - 불필요하게 큰 데이터는 DB에 저장하지 않도록 필터링해야 합니다.

8. **저장 공간 부족 처리**
   - 기기 저장 공간이 부족하면 다운로드 불가 상태를 감지해야 합니다.
   - 사용자에게 Dialog나 알림을 통해 저장 공간 부족을 안내하고, 불필요한 데이터 정리를 유도해야 합니다.

---

## 테스트

  - 아래와 같이 모든 버전에서 정상 동작하는지 테스트 진행
    
<img width="338" height="499" src="https://github.com/user-attachments/assets/a06067d4-64f7-4fbe-bcef-22ebafc26bc3" />

| | | | |
|---|---|---|---|
| <video src="https://github.com/user-attachments/assets/4aefb067-7d75-467f-8602-408af395bb1e" controls height="320"></video> | <video src="https://github.com/user-attachments/assets/af273f84-d5a0-4e14-bef9-f80b1807af14" controls height="320"></video> | <video src="https://github.com/user-attachments/assets/f6ec828a-37ea-4b59-8cc1-64f098ddbff1" controls height="320"></video> | <video src="https://github.com/user-attachments/assets/231eab15-2fab-420a-a788-18390340da1e" controls height="320"></video> |
| <video src="https://github.com/user-attachments/assets/853a0b7f-d7fd-4948-abe0-24189054baf0" controls height="320"></video> | <video src="https://github.com/user-attachments/assets/28b690a3-b16e-494a-8688-60822e42e9f9" controls height="320"></video> | <video src="https://github.com/user-attachments/assets/d2b8ee3d-6818-411d-b247-fc5358ad85c2" controls height="320"></video> | <video src="https://github.com/user-attachments/assets/408444eb-7e70-4985-8fb9-0efffb60a168" controls height="320"></video> |
| <video src="https://github.com/user-attachments/assets/006ec1b4-0bae-4a17-8714-69bc546380ec" controls height="320"></video> | <video src="https://github.com/user-attachments/assets/80a52366-8ff9-4bad-a5c9-2da53ca53fe0" controls height="320"></video> | <video src="https://github.com/user-attachments/assets/788566ee-5cf2-472c-812c-7a41768fca83" controls height="320"></video> | <video src="https://github.com/user-attachments/assets/2165dc48-ce8b-4051-beca-184cbfba09e9" controls height="320"></video> |
| <video src="https://github.com/user-attachments/assets/471b6f34-1309-4d96-aae3-8989a9fcacaa" controls height="320"></video> | <video src="https://github.com/user-attachments/assets/cf1b0f22-9191-4632-a5b2-4ad21be96ed6" controls height="320"></video> | <video src="https://github.com/user-attachments/assets/c35f4f62-bb75-49f4-95e8-1014cc3f91a5" controls height="320"></video> | <video src="https://github.com/user-attachments/assets/bc5ae96f-30b8-40d2-a10c-b747209993d0" controls height="320"></video> |


---

## 작업 로그 (12시간 30분 + 1시간 30분)

### 2025.08.15
- **13:00 ~ 15:00 (2시간)**  
  - 흑백 조절 및 밝기 증가 조절 방법과 원리 학습
- **15:00 ~ 19:00, 20:00 ~ 22:00 (총 6시간)**  
  - 요구사항 기반 흑백/밝기 조절 필터 구현
- **23:00 ~ 01:00 (2시간)**  
  - README 추가 작업 진행

### 2025.08.16
- **09:00 ~ 11:30 (2시간 30분)**  
  - README 다듬기 및 문서 내용 추가, 코드 검수
  - 버전별 테스트 및 업로드

### 2025.08.19
- **10:29 - 23:59 (1시간 30분)**  
  - ColorMatrix -> ColorFilter 변경
  - 밝기 조절 방식 변경
  - README 수정
  
