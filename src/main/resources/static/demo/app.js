const state = {
  lastRecommendationId: null
};

const accessTokenInput = document.querySelector("#accessToken");
const statusBox = document.querySelector("#status");
const clothingForm = document.querySelector("#clothingForm");
const clothesTableBody = document.querySelector("#clothesTableBody");
const clothesCount = document.querySelector("#clothesCount");
const recommendationView = document.querySelector("#recommendationView");
const recommendationMeta = document.querySelector("#recommendationMeta");
const markWornButton = document.querySelector("#markWornButton");

document.querySelector("#loadClothesButton").addEventListener("click", loadClothes);
document.querySelector("#createRecommendationButton").addEventListener("click", createRecommendation);
markWornButton.addEventListener("click", markWorn);
clothingForm.addEventListener("submit", createClothing);

window.addEventListener("DOMContentLoaded", loadClothes);

function currentAccessToken() {
  const accessToken = accessTokenInput.value.trim();
  if (!accessToken) {
    throw new Error("Bearer access token을 입력하세요.");
  }
  return accessToken;
}

async function requestJson(path, options = {}) {
  const accessToken = currentAccessToken();
  const response = await fetch(path, {
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${accessToken}`,
      ...(options.headers || {})
    },
    ...options
  });
  const body = await response.json().catch(() => null);

  if (!response.ok) {
    const message = body?.message || `HTTP ${response.status}`;
    const code = body?.code ? ` (${body.code})` : "";
    throw new Error(`${message}${code}`);
  }

  return body?.data;
}

async function loadClothes() {
  try {
    setStatus("옷 목록을 불러오는 중입니다.");
    const clothes = await requestJson("/api/clothes");
    renderClothes(clothes);
    setStatus("옷 목록을 조회했습니다.");
  } catch (error) {
    showError(error);
  }
}

async function createClothing(event) {
  event.preventDefault();

  try {
    const minTemperature = Number(document.querySelector("#minTemperature").value);
    const maxTemperature = Number(document.querySelector("#maxTemperature").value);
    if (minTemperature > maxTemperature) {
      throw new Error("최저 기온은 최고 기온보다 작거나 같아야 합니다.");
    }

    setStatus("옷을 등록하는 중입니다.");
    const payload = {
      name: document.querySelector("#name").value.trim(),
      category: document.querySelector("#category").value,
      color: document.querySelector("#color").value,
      material: document.querySelector("#material").value,
      minTemperature,
      maxTemperature,
      rainSuitable: document.querySelector("#rainSuitable").checked
    };

    await requestJson("/api/clothes", {
      method: "POST",
      body: JSON.stringify(payload)
    });
    await loadClothes();
    setStatus("옷을 등록했습니다.");
  } catch (error) {
    showError(error);
  }
}

async function createRecommendation() {
  try {
    setStatus("추천을 생성하는 중입니다.");
    const recommendation = await requestJson("/api/recommendations", {
      method: "POST"
    });

    state.lastRecommendationId = recommendation.recommendationId;
    markWornButton.disabled = recommendation.worn;
    renderRecommendation(recommendation);
    setStatus("추천을 생성했습니다.");
  } catch (error) {
    showError(error);
  }
}

async function markWorn() {
  try {
    if (!state.lastRecommendationId) {
      throw new Error("착용 완료 처리할 추천 결과가 없습니다.");
    }

    setStatus("착용 완료 처리 중입니다.");
    const worn = await requestJson(
      `/api/recommendations/${state.lastRecommendationId}/worn`,
      { method: "PATCH" }
    );

    markWornButton.disabled = true;
    recommendationMeta.textContent = `추천 #${worn.recommendationId} · 착용 완료`;
    setStatus(`추천 #${worn.recommendationId}을 착용 완료 처리했습니다.`);
  } catch (error) {
    showError(error);
  }
}

function renderClothes(clothes) {
  clothesCount.textContent = `${clothes.length}개`;
  if (clothes.length === 0) {
    clothesTableBody.innerHTML = `<tr><td colspan="6" class="empty">등록된 활성 옷이 없습니다.</td></tr>`;
    return;
  }

  clothesTableBody.innerHTML = clothes.map((item) => `
    <tr>
      <td>${escapeHtml(item.id)}</td>
      <td>${escapeHtml(item.name)}</td>
      <td>${escapeHtml(item.category)}</td>
      <td>${escapeHtml(item.color)}</td>
      <td>${escapeHtml(item.material)}</td>
      <td>${escapeHtml(item.minTemperature)}~${escapeHtml(item.maxTemperature)}도</td>
    </tr>
  `).join("");
}

function renderRecommendation(recommendation) {
  const { weather, outfit, score, reasons } = recommendation;
  recommendationMeta.textContent = `추천 #${recommendation.recommendationId} · ${recommendation.worn ? "착용 완료" : "미착용"}`;
  recommendationView.className = "recommendation";
  recommendationView.innerHTML = `
    <div class="weather-line">
      <span>${escapeHtml(weather.temperature)}도</span>
      <span>${escapeHtml(weather.weatherType)}</span>
      <span>rainy=${escapeHtml(weather.rainy)}</span>
      <span>windy=${escapeHtml(weather.windy)}</span>
    </div>
    <div class="outfit-grid">
      ${renderOutfitItem("상의", outfit.top)}
      ${renderOutfitItem("하의", outfit.bottom)}
      ${renderOutfitItem("아우터", outfit.outer)}
    </div>
    <div class="score-grid">
      ${renderScore("총점", score.totalScore)}
      ${renderScore("날씨", score.weatherScore)}
      ${renderScore("색상", score.colorScore)}
      ${renderScore("착용 이력", score.wearHistoryScore)}
      ${renderScore("추천 이력", score.recommendationHistoryScore)}
      ${renderScore("선호도", score.preferenceScore)}
    </div>
    <ol class="reasons">
      ${reasons.map((reason) => `<li>${escapeHtml(reason)}</li>`).join("")}
    </ol>
  `;
}

function renderOutfitItem(label, item) {
  if (!item) {
    return `
      <div class="outfit-item muted">
        <span>${label}</span>
        <strong>없음</strong>
      </div>
    `;
  }

  return `
    <div class="outfit-item">
      <span>${label}</span>
      <strong>${escapeHtml(item.name)}</strong>
      <small>#${escapeHtml(item.id)} · ${escapeHtml(item.color)} · ${escapeHtml(item.material)}</small>
    </div>
  `;
}

function renderScore(label, value) {
  return `
    <div class="score-item">
      <span>${label}</span>
      <strong>${escapeHtml(value)}</strong>
    </div>
  `;
}

function setStatus(message) {
  statusBox.textContent = message;
  statusBox.className = "status";
}

function showError(error) {
  statusBox.textContent = error.message;
  statusBox.className = "status error";
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
