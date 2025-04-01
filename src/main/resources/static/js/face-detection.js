const video = document.getElementById("video");
const loadingSpinner = document.getElementById("loadingSpinner");
const cameraStatus = document.getElementById("cameraStatus");
const videoContainer = document.getElementById("videoContainer");

// Disable all answer inputs and exercise links initially
function disableInteractions() {
    document.querySelectorAll(".question-card, .exercise-card").forEach(element => {
        element.style.display = "none";
    });
    document.querySelectorAll(".form-check-input, .exercise-title a").forEach(element => {
        element.disabled = true;
        element.classList.add("disabled");
    })
}

// Function to enable interactions
function enableInteractions() {
    document.querySelectorAll(".question-card, .exercise-card").forEach(element => {
        element.style.display = "block";
    });
    document.querySelectorAll(".form-check-input, .exercise-title a").forEach(element => {
        element.disabled = false;
        element.classList.remove("disabled");
    })
}

// Function to setup camera
async function setupCamera() {
    loadingSpinner.style.display = "block";
    try {
        let stream = await navigator.mediaDevices.getUserMedia({
            video: {
                width: { ideal: 640 },
                height: { ideal: 480 },
                facingMode: "user",
            },
        });
        video.srcObject = stream;

        // Wait for video metadata to load to ensure dimensions are available
        await new Promise(resolve => video.onloadedmetadata = resolve);

        // Set explicit dimensions on video element
        if (!video.width) video.width = 640
        if (!video.height) video.height = 480

        video.play();
        cameraStatus.textContent = "Camera Active";
        videoContainer.classList.add("video-active");
        enableInteractions();
    } catch (error) {
        console.error("Error accessing camera: ", error);
        alert("❌ You must allow camera access to proceed with the exam.");
        cameraStatus.textContent = "No camera";
        disableInteractions();
    } finally {
        loadingSpinner.style.display = "none";
    }
}

// Load face-api models
Promise.all([
    faceapi.nets.tinyFaceDetector.loadFromUri("/models"),
    faceapi.nets.faceLandmark68Net.loadFromUri("/models"),
    faceapi.nets.faceRecognitionNet.loadFromUri("/models"),
    faceapi.nets.faceExpressionNet.loadFromUri("/models"),
]).then(setupCamera);

// Face detection on client-side
video.addEventListener("play", function () {
    // Remove any existing canvas first to prevent duplicates
    const existingCanvas = document.querySelector(".face-detection-canvas")
    if (existingCanvas) {
        existingCanvas.remove()
    }

    // Create canvas and position it properly
    const canvas = faceapi.createCanvasFromMedia(video);
    canvas.className = "face-detection-canvas";

    // Style the canvas to overlay on top of the video
    canvas.style.position = "absolute"
    canvas.style.top = "0"
    canvas.style.left = "0"
    canvas.style.width = "100%"
    canvas.style.height = "100%"

    // Append canvas to the video container instead of body
    videoContainer.appendChild(canvas)

    // Get actual dimensions from the video element's computed style
    // This ensures we have valid dimensions even if not set directly on the video
    const videoWidth = video.clientWidth || video.offsetWidth || 320
    const videoHeight = video.clientHeight || video.offsetHeight || 240

    const displaySize = { width: videoWidth, height: videoHeight }
    faceapi.matchDimensions(canvas, displaySize);

    // Track face detection violations
    let noFaceDetectedCount = 0;
    let multipleFacesDetectedCount = 0;

    let previousMultipleFacesDetected = false;
    let previousNoFaceDetected = false;

    setInterval(async () => {
        // Cập nhật kích thước video để vẽ chính xác
        const currentWidth = video.clientWidth || video.offsetWidth || 320;
        const currentHeight = video.clientHeight || video.offsetHeight || 240;
        const currentDisplaySize = { width: currentWidth, height: currentHeight };

        if (displaySize.width !== currentWidth || displaySize.height !== currentHeight) {
            faceapi.matchDimensions(canvas, currentDisplaySize);
        }

        // Phát hiện khuôn mặt
        const detections = await faceapi
            .detectAllFaces(video, new faceapi.TinyFaceDetectorOptions())
            .withFaceLandmarks()
            .withFaceExpressions();

        const resizedDetections = faceapi.resizeResults(detections, currentDisplaySize);
        canvas.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);

        // Vẽ lên canvas
        faceapi.draw.drawDetections(canvas, resizedDetections);

        // Kiểm tra số khuôn mặt phát hiện được
        let multipleFacesDetected = detections.length > 1;
        let noFaceDetected = detections.length === 0;

        // Xử lý khi có nhiều khuôn mặt
        if (multipleFacesDetected && !previousMultipleFacesDetected) {
            violationFaceCount++;
            sessionStorage.setItem("violationFaceCount", violationFaceCount.toString());
        }

        // Xử lý khi không có khuôn mặt
        if (noFaceDetected && !previousNoFaceDetected) {
            violationFaceCount++;
            sessionStorage.setItem("violationFaceCount", violationFaceCount.toString());
        }

        // Cập nhật trạng thái
        previousMultipleFacesDetected = multipleFacesDetected;
        previousNoFaceDetected = noFaceDetected;

        // Hiển thị trạng thái
        if (multipleFacesDetected) {
            cameraStatus.textContent = `⚠️ Multiple faces detected (${violationFaceCount})`;
            cameraStatus.style.backgroundColor = "rgba(255, 0, 0, 0.7)";
        } else if (noFaceDetected) {
            cameraStatus.textContent = `⚠️ Face not detected (${violationFaceCount})`;
            cameraStatus.style.backgroundColor = "rgba(255, 0, 0, 0.7)";
        } else {
            cameraStatus.textContent = "Camera Active";
            cameraStatus.style.backgroundColor = "rgba(0, 0, 0, 0.7)";
        }
    }, 100);


})

// Make sure canvas resizes with video
window.addEventListener("resize", () => {
    const canvas = document.querySelector(".face-detection-canvas")
    if (canvas && video) {
        const displaySize = { width: video.clientWidth, height: video.clientHeight }
        faceapi.matchDimensions(canvas, displaySize)
    }
})

