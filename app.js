const grid = document.getElementById("grid");
const mineCount = document.getElementById("mineCount");
const mineValue = document.getElementById("mineValue");
const betInput = document.getElementById("betInput");
const betValue = document.getElementById("betValue");
const winValue = document.getElementById("winValue");
const startButton = document.getElementById("startButton");
const cashoutButton = document.getElementById("cashoutButton");
const overlay = document.getElementById("overlay");
const overlayMessage = document.getElementById("overlayMessage");
const restartButton = document.getElementById("restartButton");

const gridSize = 25;
let mines = new Set();
let revealed = new Set();
let running = false;
let winnings = 0;
let gameReady = false;

function createGrid() {
  grid.innerHTML = "";
  for (let i = 0; i < gridSize; i += 1) {
    const tile = document.createElement("button");
    tile.className = "tile";
    tile.type = "button";
    tile.dataset.index = i.toString();
    tile.addEventListener("click", () => handleTileClick(i, tile));
    grid.appendChild(tile);
  }
}

function setupGame() {
  mines = new Set();
  revealed = new Set();
  winnings = 0;
  winValue.textContent = "0";
  betValue.textContent = betInput.value;
  overlay.hidden = true;
  cashoutButton.disabled = false;
  running = true;
  gameReady = true;

  while (mines.size < Number(mineCount.value)) {
    mines.add(Math.floor(Math.random() * gridSize));
  }

  document.querySelectorAll(".tile").forEach((tile) => {
    tile.className = "tile";
    tile.textContent = "";
    tile.disabled = false;
  });
}

function handleTileClick(index, tile) {
  if (!running || revealed.has(index)) {
    return;
  }

  revealed.add(index);
  if (mines.has(index)) {
    tile.classList.add("tile--mine");
    tile.textContent = "💣";
    endGame(false);
    return;
  }

  tile.classList.add("tile--safe");
  tile.textContent = "💎";
  winnings += Number(betInput.value) * 0.2;
  winValue.textContent = winnings.toFixed(0);
}

function endGame(win) {
  running = false;
  cashoutButton.disabled = true;
  document.querySelectorAll(".tile").forEach((tile) => {
    tile.disabled = true;
    const index = Number(tile.dataset.index);
    if (mines.has(index)) {
      tile.classList.add("tile--mine");
      tile.textContent = "💣";
    }
  });
  overlayMessage.textContent = win
    ? `Вы забрали ${winnings.toFixed(0)}!`
    : "Бум! Вы наткнулись на мину.";
  overlay.hidden = false;
}

function cashOut() {
  if (!running) {
    return;
  }
  endGame(true);
}

function setIdleState(message) {
  overlayMessage.textContent = message;
  overlay.hidden = false;
  cashoutButton.disabled = true;
  running = false;
  document.querySelectorAll(".tile").forEach((tile) => {
    tile.disabled = true;
    tile.className = "tile";
    tile.textContent = "";
  });
}

mineCount.addEventListener("input", () => {
  mineValue.textContent = mineCount.value;
  if (gameReady) {
    setupGame();
  }
});

startButton.addEventListener("click", setupGame);
restartButton.addEventListener("click", setupGame);

betInput.addEventListener("input", () => {
  betValue.textContent = betInput.value;
});

cashoutButton.addEventListener("click", cashOut);

createGrid();
setIdleState("Нажмите «Старт», чтобы начать раунд.");
