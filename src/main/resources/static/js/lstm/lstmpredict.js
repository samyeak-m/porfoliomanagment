class StockDropdown {
  constructor() {
    this.stockInput = document.getElementById("stockSymbol");
    this.dropdownContainer = document.getElementById("stockDropdownList");
    this.stockSymbols = [];
    this.selectedIndex = -1;
    this.init();
  }

  async init() {
    await this.loadStockSymbols();
    this.bindEvents();
  }

  async loadStockSymbols() {
    const DEBOUNCE_MS = 300;
    this._debounceTimer = null;

    this.handleInput = (e) => {
      const query = e.target.value.trim().toUpperCase();
      if (this._debounceTimer) clearTimeout(this._debounceTimer);

      if (!query) {
        this.stockSymbols = [];
        this.hideDropdown();
        return;
      }

      this._debounceTimer = setTimeout(async () => {
        try {
          const resp = await fetch(`/api/stock/symbols?q=${encodeURIComponent(query)}&limit=20`);
          if (!resp.ok) {
            this.hideDropdown();
            return;
          }
          const list = await resp.json();
          // Keep only symbols that start with the typed prefix
          const symbols = (Array.isArray(list) ? list : [])
            .filter(s => typeof s === 'string' && s.toUpperCase().startsWith(query))
            .slice(0, 10);

          this.stockSymbols = symbols;
          this.renderDropdown(symbols);
          this.selectedIndex = -1;
        } catch (err) {
          console.error('Error fetching symbols:', err);
          this.hideDropdown();
        }
      }, DEBOUNCE_MS);
    };
  }

  bindEvents() {
    this.stockInput.addEventListener("input", (e) => this.handleInput(e));
    this.stockInput.addEventListener("keydown", (e) => this.handleKeydown(e));
    this.stockInput.addEventListener("focus", () => {
      if (this.stockInput.value.trim().length > 0) {
        this.handleInput({ target: this.stockInput });
      }
    });

    this.stockInput.addEventListener("blur", (e) => {
      setTimeout(() => {
        const activeElement = document.activeElement;
        if (
          !this.dropdownContainer.contains(activeElement) &&
          activeElement !== this.stockInput
        ) {
          this.hideDropdown();
        }
      }, 100);
    });

    document.addEventListener("click", (e) => {
      if (
        !this.stockInput.contains(e.target) &&
        !this.dropdownContainer.contains(e.target)
      ) {
        this.hideDropdown();
      }
    });

    window.addEventListener("resize", () => {
      if (this.dropdownContainer.style.display === "block") {
        this.showDropdown();
      }
    });

    window.addEventListener("scroll", () => {
      if (this.dropdownContainer.style.display === "block") {
        this.showDropdown();
      }
    });
  }

  handleInput(e) {
    const query = e.target.value.trim().toUpperCase();
    if (query.length === 0) {
      this.hideDropdown();
      return;
    }

    const filteredSymbols = this.stockSymbols
      .filter((symbol) => symbol.toUpperCase().includes(query))
      .slice(0, 10);

    this.renderDropdown(filteredSymbols);
    this.selectedIndex = -1;
  }

  handleKeydown(e) {
    const visibleItems =
      this.dropdownContainer.querySelectorAll(".dropdown-item");

    switch (e.key) {
      case "ArrowDown":
        e.preventDefault();
        this.selectedIndex = Math.min(
          this.selectedIndex + 1,
          visibleItems.length - 1
        );
        this.updateSelection(visibleItems);
        break;

      case "ArrowUp":
        e.preventDefault();
        this.selectedIndex = Math.max(this.selectedIndex - 1, -1);
        this.updateSelection(visibleItems);
        break;

      case "Enter":
        e.preventDefault();
        if (this.selectedIndex >= 0 && visibleItems[this.selectedIndex]) {
          this.selectSymbol(visibleItems[this.selectedIndex].textContent);
        }
        break;

      case "Escape":
        this.hideDropdown();
        break;
    }
  }

  updateSelection(items) {
    items.forEach((item, index) => {
      item.classList.toggle("selected", index === this.selectedIndex);
    });
  }

  renderDropdown(symbols) {
    if (symbols.length === 0) {
      this.hideDropdown();
      return;
    }

    this.dropdownContainer.innerHTML = symbols
      .map(symbol => `
      <div class="dropdown-item" onclick="stockDropdown.selectSymbol('${symbol}')" onmousedown="event.preventDefault()">
        <div class="stock-icon">${symbol.substring(0, 2)}</div>
        <span class="stock-symbol">${symbol}</span>
      </div>
    `)
      .join('');

    this.showDropdown();
  }

  selectSymbol(symbol) {
    this.stockInput.value = symbol;
    this.hideDropdown();
    this.stockInput.focus();
    this.stockInput.dispatchEvent(new Event("change"));
  }

  showDropdown() {
    if (this.dropdownContainer.children.length > 0) {
      this.dropdownContainer.style.display = "block";
      const inputRect = this.stockInput.getBoundingClientRect();
      this.dropdownContainer.style.width = inputRect.width + "px";
    }
  }

  hideDropdown() {
    this.dropdownContainer.style.display = "none";
    this.selectedIndex = -1;
  }
}

class CompactPredictionForm {
  constructor() {
    this.form = document.getElementById("predict-form");
    this.button = document.getElementById("predictBtn");
    this.resultsDiv = document.getElementById("predictionResults");
    this.bindEvents();
  }

  bindEvents() {
    this.form.addEventListener("submit", (e) => this.handleSubmit(e));
  }

  async handleSubmit(e) {
    e.preventDefault();

    const stockSymbol = document
      .getElementById("stockSymbol")
      .value.trim()
      .toUpperCase();

    if (!stockSymbol) {
      this.showError("Please enter a stock symbol");
      return;
    }

    this.setLoadingState(true);

    try {
      const requestData = {
        stockSymbol: stockSymbol
      };

      const response = await fetch("/api/lstm/predict", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(requestData),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();
      this.showResults(data);
    } catch (error) {
      this.showError("Unable to predict. Please try again. " + error.message);
    } finally {
      this.setLoadingState(false);
    }
  }

  setLoadingState(loading) {
    this.button.disabled = loading;
    this.button.innerHTML = loading
        ? '<div class="spinner"></div><span>Analyzing...</span>'
        : '<span>Analyze Stock</span>';
  }

  showResults(data) {
    const isPositive = data.pointChange > 0;
    const changeClass = isPositive ? "positive" : "negative";

    this.resultsDiv.innerHTML = `
            <div class="results-header">
                <h3>${data.stockSymbol} Prediction</h3>
            </div>
            <div class="results-body">
                <div class="section-title">
                    AI Price Prediction
                </div>
                <div class="result-grid">
                    <div class="result-item">
                        <div class="result-label">Predicted Price</div>
                        <div class="result-value">NPR ${data.prediction.toFixed(2)}</div>
                    </div>
                    <div class="result-item">
                        <div class="result-label">Last Close</div>
                        <div class="result-value">NPR ${data.lastClose.toFixed(2)}</div>
                    </div>
                    <div class="result-item">
                        <div class="result-label">Point Change</div>
                        <div class="result-value ${changeClass}">
                            <span class="price-indicator ${changeClass}">
                                ${data.pointChange > 0 ? "+" : ""}${data.pointChange.toFixed(2)}
                            </span>
                        </div>
                    </div>
                    <div class="result-item">
                        <div class="result-label">Price Change %</div>
                        <div class="result-value ${changeClass}">
                            <span class="price-indicator ${changeClass}">
                                ${data.priceChange > 0 ? "+" : ""}${data.priceChange.toFixed(2)}%
                            </span>
                        </div>
                    </div>
                </div>
            </div>
        `;

    this.resultsDiv.style.display = "block";
    this.resultsDiv.scrollIntoView({ behavior: "smooth" });
  }

  showError(message) {
    this.resultsDiv.innerHTML = `
            <div class="error-message">
                <span>${message}</span>
            </div>
        `;
    this.resultsDiv.style.display = "block";
  }
}

// Initialize both classes
let stockDropdown;
let formHandler;
document.addEventListener("DOMContentLoaded", function () {
  // Only init dropdown if inputs exist (works on predict and VaR pages)
  const inputEl = document.getElementById("stockSymbol");
  const listEl = document.getElementById("stockDropdownList");
  if (inputEl && listEl) {
    stockDropdown = new StockDropdown();
    // expose globally for inline onclick handlers
    window.stockDropdown = stockDropdown;
  }
  // Only init prediction form on predict page
  const formEl = document.getElementById("predict-form");
  if (formEl) {
    formHandler = new CompactPredictionForm();
  }
});
