const state = {
    apiKey: 'AIzaSyBDdQmr9uRtDHuBVMfFuTEQe3kHPQx4b20',
    sessionId: null,
    isProcessing: false,
    currentFile: null,
    analysisCnt: 0,
    history: []
};

const SYSTEM_PROMPT = `You are NEURO-AI, an advanced Brain MRI Analysis Engine. 
STRICT ZERO-HALLUCINATION POLICY. If image quality is poor, say so.
ANALYSIS PROTOCOL:
1. QUALITY ASSESSMENT
2. ANOMALY DETECTION
3. TUMOR CHARACTERIZATION (if detected)
4. DIFFERENTIAL DIAGNOSIS
5. CLINICAL RECOMMENDATION

Respond ONLY with valid JSON:
{
  "imageQuality": "Good|Fair|Poor",
  "tumorDetected": true|false,
  "confidenceScore": 0-100,
  "tumorFindings": { "location": "string", "size": "string" },
  "likelyDiagnosis": "string",
  "regionHighlight": { "approximateX": 0-100, "approximateY": 0-100, "description": "string" },
  "keyMRIFeatures": ["string"],
  "clinicalRecommendation": "string",
  "urgencyLevel": "Routine|Urgent|Emergency"
}
STRICT SPATIAL ACCURACY: Ensure regionHighlight coordinates (approximateX, approximateY) correspond precisely to the lesion's center point (0-100 scale).`;

document.addEventListener('DOMContentLoaded', () => {
    initUI();
});

function initUI() {
    const fileInput = document.getElementById('fileInput');
    const uploadBtn = document.getElementById('uploadBtn');
    const sendBtn = document.getElementById('sendBtn');
    const chatInput = document.getElementById('chatInput');

    uploadBtn.onclick = () => fileInput.click();
    fileInput.onchange = handleFileSelect;
    sendBtn.onclick = handleSend;
    chatInput.onkeydown = (e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend(); } };
}

function handleFileSelect(e) {
    const file = e.target.files[0];
    if (!file) return;
    state.currentFile = file;

    const reader = new FileReader();
    reader.onload = (ev) => {
        document.getElementById('previewThumb').src = ev.target.result;
        document.getElementById('uploadPreview').style.display = 'flex';
        document.getElementById('welcomeScreen').style.display = 'none';
    };
    reader.readAsDataURL(file);
}

function clearUpload() {
    state.currentFile = null;
    document.getElementById('fileInput').value = '';
    document.getElementById('uploadPreview').style.display = 'none';
}

async function handleSend() {
    if (state.isProcessing) return;
    const text = document.getElementById('chatInput').value.trim();
    if (!text && !state.currentFile) return;

    setProcessing(true);
    const context = text;
    const file = state.currentFile;

    addUserMessage(text, file);
    document.getElementById('chatInput').value = '';
    clearUpload();

    const typingId = showTyping();

    try {
        let result;
        if (file) {
            const base64 = await fileToBase64(file);
            result = await callLocalVision(base64, file.type, context);
            renderAnalysisReport(result, base64, file.type);
            state.analysisCnt++;
            document.getElementById('analysisCnt').textContent = state.analysisCnt;
            document.getElementById('lastConf').textContent = result.confidenceScore + '%';
        } else {
            result = await callLocalText(context);
            addAITextMessage(result);
        }
    } catch (err) {
        addErrorMessage('Analysis failed: ' + err.message);
    } finally {
        removeTyping(typingId);
        setProcessing(false);
    }
}

async function callLocalVision(base64, mimeType, prompt) {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${state.apiKey}`;
    const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            contents: [{
                parts: [
                    { text: SYSTEM_PROMPT + "\n\nUser Question: " + (prompt || "Analyze this scan.") },
                    { inlineData: { mimeType, data: base64 } }
                ]
            }]
        })
    });

    // Verify we received a JSON response; otherwise throw a helpful error.
    const contentType = response.headers.get('content-type') || '';
    if (!response.ok) {
        const errText = await response.text();
        throw new Error(`Vision API request failed (${response.status}): ${errText}`);
    }
    if (!contentType.includes('application/json')) {
        const errText = await response.text();
        throw new Error(`Unexpected response format (${contentType}): ${errText}`);
    }
    const data = await response.json();
    const raw = data.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!raw) {
        throw new Error('Invalid response structure from CLINIX AI API');
    }
    // Strip any markdown fences before parsing JSON.
    return JSON.parse(raw.replace(/```json\n?|```/g, ''));
}

async function callLocalText(prompt) {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${state.apiKey}`;
    const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            contents: [{ parts: [{ text: "You are NEURO-AI medical assistant. Answer concisely: " + prompt }] }]
        })
    });
    const data = await response.json();
    return data.candidates[0].content.parts[0].text;
}

function addUserMessage(text, file) {
    const container = document.getElementById('chatMessages');
    const msg = document.createElement('div');
    msg.className = 'message user';
    msg.innerHTML = `
        <div class="avatar user-av">👤</div>
        <div class="bubble">
            <div class="bubble-content user-bubble">
                ${file ? `<img src="${URL.createObjectURL(file)}" style="max-width:200px; border-radius:8px; display:block; margin-bottom:10px;">` : ''}
                ${text || 'Initiating Analysis...'}
            </div>
        </div>
    `;
    container.appendChild(msg);
    container.scrollTop = container.scrollHeight;
}

function showTyping() {
    const id = 'typing_' + Date.now();
    const container = document.getElementById('chatMessages');
    const msg = document.createElement('div');
    msg.id = id;
    msg.className = 'message';
    msg.innerHTML = `
        <div class="avatar ai">🧠</div>
        <div class="bubble">
            <div class="bubble-content ai-bubble">Analyzing neural parameters...</div>
        </div>
    `;
    container.appendChild(msg);
    container.scrollTop = container.scrollHeight;
    return id;
}

function removeTyping(id) {
    const el = document.getElementById(id);
    if (el) el.remove();
}

function renderAnalysisReport(data, base64, mimeType) {
    const container = document.getElementById('chatMessages');
    const canvId = 'canv_' + Date.now();
    const msg = document.createElement('div');
    msg.className = 'message';
    msg.innerHTML = `
        <div class="avatar ai">🧠</div>
        <div class="bubble" style="width: 100%;">
            <div class="report-card">
                <div class="report-header ${data.tumorDetected ? 'tumor-detected' : 'no-tumor'}">
                    <div style="font-family: var(--font-mono); font-size: 11px;">BRAIN MRI REPORT</div>
                    <div style="background: rgba(0,0,0,0.2); padding: 4px 8px; border-radius: 4px; font-weight: 700; font-size: 10px;">
                        ${data.tumorDetected ? '⚠️ ANOMALY DETECTED' : '✓ CLEAR SCAN'}
                    </div>
                </div>
                <div style="padding: 15px;">
                    <div style="display:flex; justify-content:space-between; margin-bottom:5px;">
                        <span style="font-family:var(--font-mono); font-size:10px; color:var(--text-muted);">CONFIDENCE</span>
                        <span style="color:var(--accent-cyan); font-weight:700;">${data.confidenceScore}%</span>
                    </div>
                    <div class="meter-track">
                        <div class="meter-fill high" style="width: ${data.confidenceScore}%"></div>
                    </div>
                </div>
                <div style="padding: 15px; border-top: 1px solid var(--border); text-align:center;">
                    <div class="mri-canvas-container">
                        <img id="img_${canvId}" src="data:${mimeType};base64,${base64}" style="max-height:220px;">
                        <canvas id="${canvId}"></canvas>
                    </div>
                </div>
                <div style="padding: 15px; border-top: 1px solid var(--border);">
                    <div style="font-size: 14px; color: var(--accent-cyan); font-weight: 700; margin-bottom: 5px;">${data.likelyDiagnosis}</div>
                    <div style="font-size: 12px; color: var(--text-secondary); line-height: 1.6;">${data.clinicalRecommendation}</div>
                </div>
            </div>
        </div>
    `;
    container.appendChild(msg);
    container.scrollTop = container.scrollHeight;

    setTimeout(() => drawOverlay(canvId, data.regionHighlight), 100);
}

function drawOverlay(id, rh) {
    const img = document.getElementById('img_' + id);
    const canv = document.getElementById(id);
    if (!img || !canv || !rh) return;

    canv.width = img.clientWidth;
    canv.height = img.clientHeight;
    const ctx = canv.getContext('2d');

    const x = (rh.approximateX / 100) * canv.width;
    const y = (rh.approximateY / 100) * canv.height;

    ctx.strokeStyle = '#ff3b5c';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.arc(x, y, 25, 0, Math.PI * 2);
    ctx.stroke();

    ctx.fillStyle = 'rgba(255, 59, 92, 0.2)';
    ctx.fill();

    ctx.fillStyle = '#ff3b5c';
    ctx.font = 'bold 10px monospace';
    ctx.fillText('▲ ANOMALY', x + 30, y);
}

function addAITextMessage(text) {
    const container = document.getElementById('chatMessages');
    const msg = document.createElement('div');
    msg.className = 'message';
    msg.innerHTML = `
        <div class="avatar ai">🧠</div>
        <div class="bubble">
            <div class="bubble-content ai-bubble">${text}</div>
        </div>
    `;
    container.appendChild(msg);
    container.scrollTop = container.scrollHeight;
}

function addErrorMessage(text) {
    addAITextMessage('<span style="color:var(--accent-red)">' + text + '</span>');
}

function setProcessing(val) {
    state.isProcessing = val;
    document.getElementById('sendBtn').style.opacity = val ? '0.3' : '1';
}

function fileToBase64(file) {
    return new Promise((r, j) => {
        const reader = new FileReader();
        reader.onload = () => r(reader.result.split(',')[1]);
        reader.readAsDataURL(file);
    });
}
