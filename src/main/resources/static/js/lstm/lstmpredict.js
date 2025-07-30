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
    try {
      const response = await fetch("/api/lstm/stock-symbols");
      if (response.ok) {
        this.stockSymbols = await response.json();
        console.log("Loaded stock symbols:", this.stockSymbols.length);
      } else {
        console.error("Failed to load stock symbols");
      }
    } catch (error) {
      console.error("Error loading stock symbols:", error);
    }
  }

  bindEvents() {
    this.stockInput.addEventListener("input", (e) => this.handleInput(e));
    this.stockInput.addEventListener("keydown", (e) => this.handleKeydown(e));
    this.stockInput.addEventListener("focus", () => {
      if (this.stockInput.value.trim().length > 0) {
        this.handleInput({ target: this.stockInput });
      }
    });

    // Better blur handling
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

    // Handle clicks outside
    document.addEventListener("click", (e) => {
      if (
        !this.stockInput.contains(e.target) &&
        !this.dropdownContainer.contains(e.target)
      ) {
        this.hideDropdown();
      }
    });

    // Handle window resize and scroll to reposition dropdown
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
      .slice(0, 10); // Limit to 10 results

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

    this.dropdownContainer.innerHTML = '';

    if (symbols.length === 0) {
        this.dropdownContainer.innerHTML = `
            <div class="dropdown-no-results">
                No stocks found matching your search
            </div>
        `;
    } else {
        this.dropdownContainer.innerHTML = symbols
            .map(symbol => `
                <div class="dropdown-item" onclick="stockDropdown.selectSymbol('${symbol}')" onmousedown="event.preventDefault()">
                    <div class="stock-icon">${symbol.substring(0, 2)}</div>
                    <span class="stock-symbol">${symbol}</span>
                </div>
            `)
            .join('');
    }

    this.showDropdown();
  }

  selectSymbol(symbol) {
    this.stockInput.value = symbol;
    this.hideDropdown();
    this.stockInput.focus();

    // Trigger change event for any listeners
    this.stockInput.dispatchEvent(new Event("change"));
  }

  showDropdown() {
    if (this.dropdownContainer.children.length > 0) {
      this.dropdownContainer.style.display = "block";

      // Get the actual position of the input field relative to the viewport
      const inputRect = this.stockInput.getBoundingClientRect();
      const containerRect = document
        .getElementById("stockDropdown")
        .getBoundingClientRect();

      // Position dropdown outside the container using fixed positioning
      this.dropdownContainer.style.position = "fixed";
      this.dropdownContainer.style.top = inputRect.bottom + 4 + "px";
      this.dropdownContainer.style.left = inputRect.left + "px";
      this.dropdownContainer.style.width = inputRect.width + "px";
      this.dropdownContainer.style.zIndex = "9999";

      // Remove any previous positioning styles
      this.dropdownContainer.style.bottom = "auto";
      this.dropdownContainer.style.right = "auto";

      // Check if dropdown would go below viewport
      const viewportHeight = window.innerHeight;
      const dropdownHeight = 250;

      if (inputRect.bottom + dropdownHeight > viewportHeight - 10) {
        // Position above input if no space below
        this.dropdownContainer.style.top =
          inputRect.top - dropdownHeight - 4 + "px";
      }
    }
  }

  hideDropdown() {
    this.dropdownContainer.style.display = "none";
    this.selectedIndex = -1;

    // Reset positioning to original state
    this.dropdownContainer.style.position = "absolute";
    this.dropdownContainer.style.top = "calc(100% + 4px)";
    this.dropdownContainer.style.left = "0";
    this.dropdownContainer.style.width = "100%";
    this.dropdownContainer.style.zIndex = "1001";
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

    // Get period value and unit
    const periodValue = parseInt(document.getElementById("periodValue").value);
    const periodUnit = document.getElementById("periodUnit").value;

    // Convert to days
    let daysOfInvestment = periodValue;
    if (periodUnit === "weeks") daysOfInvestment *= 7;
    else if (periodUnit === "months") daysOfInvestment *= 30;
    else if (periodUnit === "years") daysOfInvestment *= 365;

    // Get other VaR config fields as needed
    const confidenceLevel =
      document.getElementById("confidenceLevel")?.value || "0.95";
    const numSimulations =
      parseInt(document.getElementById("numSimulations")?.value) || 10000;

    if (!stockSymbol) {
      this.showError("Please enter a stock symbol");
      return;
    }
    if (daysOfInvestment < 1) {
      this.showError("Investment period must be at least 1 day");
      return;
    }

    this.setLoadingState(true);

    try {
      const requestData = {
        stockSymbol: stockSymbol,
        daysOfInvestment: daysOfInvestment,
        confidenceLevel: confidenceLevel,
        numSimulations: numSimulations,
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
        : '<span>Analyze Stock & Calculate Risk</span>';
  }

  showResults(data) {
    const isPositive = data.pointChange > 0;
    const changeClass = isPositive ? "positive" : "negative";

    // Determine risk level based on VaR percentage
    const riskLevel = this.getRiskLevel(data.varPercentage);
    const riskClass = this.getRiskClass(data.varPercentage);

    this.resultsDiv.innerHTML = `
            <div class="results-header">
                <h3>${data.stockSymbol} Complete Analysis</h3>
            </div>
            <div class="results-body">
                <!-- LSTM Prediction Section -->
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

                <!-- VaR Risk Analysis Section -->
                <div class="var-results-section">
                    <div class="section-title">
                        Value at Risk (VaR) Analysis
                    </div>
                    
                    <div class="var-summary">
                        <div class="var-metric">
                            <div class="var-metric-label">Value at Risk</div>
                            <div class="var-metric-value ${riskClass}">NPR ${data.varValue.toFixed(2)}</div>
                        </div>
                        <div class="var-metric">
                            <div class="var-metric-label">Risk Percentage</div>
                            <div class="var-metric-value ${riskClass}">${data.varPercentage.toFixed(2)}%</div>
                        </div>
                        <div class="var-metric">
                            <div class="var-metric-label">Confidence Level</div>
                            <div class="var-metric-value">${data.confidenceLevel.toFixed(1)}%</div>
                        </div>
                        <div class="var-metric">
                            <div class="var-metric-label">Risk Category</div>
                            <div class="var-metric-value ${riskClass}">
                                <span class="risk-badge ${riskClass}">${riskLevel}</span>
                            </div>
                        </div>
                    </div>

                    <!-- Simple Risk Gauge -->
                    <div class="risk-gauge">
                        <div class="gauge-container">
                            <div class="gauge-needle" style="transform: translateX(-50%) rotate(${this.calculateGaugeAngle(data.varPercentage)}deg)"></div>
                        </div>
                        
                        <div class="gauge-labels">
                            <div class="gauge-label low">Low<br>0-10%</div>
                            <div class="gauge-label medium">Medium<br>10-20%</div>
                            <div class="gauge-label high">High<br>20%+</div>
                        </div>
                    </div>
                </div>

                <!-- Investment Recommendation -->
                <div class="recommendation-section">
                    <div class="section-title">
                        Investment Recommendation
                    </div>
                    <div class="recommendation-card ${riskClass}">
                        ${this.getInvestmentRecommendation(data)}
                    </div>
                </div>
            </div>
        `;

    this.resultsDiv.style.display = "block";
    this.resultsDiv.scrollIntoView({ behavior: "smooth" });
  }

  calculateGaugeAngle(varPercentage) {
    // Convert 0-30% risk to 0-180 degrees (semicircle)
    const maxRisk = 30;
    const normalizedRisk = Math.min(varPercentage, maxRisk) / maxRisk;
    
    // Map to -90 to +90 degrees (180 degree range)
    // -90 = low risk (left), 0 = medium risk (center), +90 = high risk (right)
    const angle = (normalizedRisk * 180) - 90;
    
    return angle;
  }

  getRiskLevel(varPercentage) {
    if (varPercentage <= 5) return "Low Risk";
    if (varPercentage <= 10) return "Medium Risk";
    if (varPercentage <= 20) return "High Risk";
    return "Very High Risk";
  }

  getRiskClass(varPercentage) {
    if (varPercentage <= 5) return "risk-low";
    if (varPercentage <= 10) return "risk-medium";
    if (varPercentage <= 20) return "risk-high";
    return "risk-high";
  }

  getInvestmentRecommendation(data) {
    const riskPercentage = data.varPercentage;
    const priceChange = data.priceChange;

    let recommendation = "";

    if (riskPercentage <= 5 && priceChange > 2) {
      recommendation = "Strong Buy - Low risk with positive prediction";
    } else if (riskPercentage <= 10 && priceChange > 0) {
      recommendation = "Buy - Moderate risk with upward trend";
    } else if (riskPercentage <= 10 && priceChange < 0) {
      recommendation = "Hold - Moderate risk with downward prediction";
    } else if (riskPercentage > 20) {
      recommendation = "Avoid - High risk investment";
    } else {
      recommendation = "Caution - Analyze market conditions carefully";
    }

    return `
            <div class="recommendation-content">
                <span>${recommendation}</span>
            </div>
            <div class="recommendation-details">
                <small>Based on ${data.confidenceLevel.toFixed(1)}% confidence level over ${data.daysOfInvestment || 25} days</small>
            </div>
        `;
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
  stockDropdown = new StockDropdown();
  formHandler = new CompactPredictionForm();
});
