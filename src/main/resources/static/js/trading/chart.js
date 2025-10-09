// Wait for both DOM and TradingView library to be ready
function initializeChart() {
    // Check if library is loaded
    if (typeof LightweightCharts === 'undefined') {
        console.error('TradingView Lightweight Charts library not loaded');
        showLibraryError();
        setTimeout(initializeChart, 500);
        return;
    }
    
    console.log('Initializing TradingChart with LightweightCharts version:', LightweightCharts.version);
    
    try {
        const chart = new TradingChart();
        chart.init();
    } catch (error) {
        console.error('Failed to initialize TradingChart:', error);
        showInitializationError(error);
    }
}

function showLibraryError() {
    const container = document.querySelector('.trading-container');
    if (container && !document.getElementById('library-error')) {
        const errorDiv = document.createElement('div');
        errorDiv.id = 'library-error';
        errorDiv.style.cssText = 'background: rgba(239, 68, 68, 0.1); border: 1px solid rgba(239, 68, 68, 0.3); border-radius: 12px; padding: 2rem; margin: 2rem; text-align: center; color: rgba(255, 255, 255, 0.9);';
        errorDiv.innerHTML = `
            <h3>Loading Chart Library...</h3>
            <p>Please wait while we load the charting components.</p>
            <div class="spinner" style="display: inline-block; margin-top: 1rem;"></div>
        `;
        container.insertBefore(errorDiv, container.firstChild);
    }
}

function showInitializationError(error) {
    const container = document.querySelector('.trading-container');
    if (container) {
        const errorDiv = document.createElement('div');
        errorDiv.style.cssText = 'background: rgba(239, 68, 68, 0.1); border: 1px solid rgba(239, 68, 68, 0.3); border-radius: 12px; padding: 2rem; margin: 2rem; text-align: center; color: rgba(255, 255, 255, 0.9);';
        errorDiv.innerHTML = `
            <h3>Failed to Initialize Charts</h3>
            <p>Error: ${error.message}</p>
            <p>Please refresh the page or contact support if the problem persists.</p>
            <button onclick="location.reload()" style="margin-top: 1rem; padding: 0.75rem 1.5rem; background: rgba(255, 255, 255, 0.15); border: 1px solid rgba(255, 255, 255, 0.3); border-radius: 8px; color: white; cursor: pointer;">
                Refresh Page
            </button>
        `;
        container.insertBefore(errorDiv, container.firstChild);
    }
}

class TradingChart {
    constructor() {
        this.mainChart = null;
        this.volumeChart = null;
        this.indicatorChart = null; // Re-add indicator chart
        this.candlestickSeries = null;
        this.volumeSeries = null;
        this.selectedSymbol = '';
        this.chartType = 'candlestick';
        this.seriesMap = new Map();
        // Re-add indicators tracking
        this.indicators = {
            sma20: false,
            sma50: false,
            ema12: false,
            ema26: false,
            bollinger: false,
            rsi: false,
            macd: false
        };
        this.indicatorSeriesList = [];
    }

    async init() {
        if (typeof LightweightCharts === 'undefined') {
            throw new Error('LightweightCharts library is not available');
        }

        const errorEl = document.getElementById('library-error');
        if (errorEl) {
            errorEl.remove();
        }

        this.setupCharts();
        this.bindEvents();
        await this.loadSymbols();
    }

    async loadSymbols() {
        try {
            const response = await fetch('/trading/api/symbols');
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            
            const symbols = await response.json();
            
            const select = document.getElementById('symbolSelect');
            select.innerHTML = '<option value="">Select Symbol...</option>';
            
            symbols.forEach(symbol => {
                const option = document.createElement('option');
                option.value = symbol;
                option.textContent = symbol;
                select.appendChild(option);
            });

            if (symbols.length > 0) {
                select.value = symbols[0];
                this.selectedSymbol = symbols[0];
                await this.loadChartData();
            } else {
                console.warn('No symbols available');
                this.showMessage('No stock symbols available', 'warning');
            }
        } catch (error) {
            console.error('Error loading symbols:', error);
            this.showMessage('Failed to load stock symbols: ' + error.message, 'error');
        }
    }

    setupCharts() {
        const chartContainer = document.getElementById('mainChart');
        const volumeContainer = document.getElementById('volumeChart');
        const indicatorContainer = document.getElementById('indicatorChart');

        if (!chartContainer || !volumeContainer || !indicatorContainer) {
            throw new Error('Chart containers not found in DOM');
        }

        if (!LightweightCharts || !LightweightCharts.createChart) {
            throw new Error('LightweightCharts.createChart is not a function');
        }

        try {
            console.log('Creating main chart...');
            
            this.mainChart = LightweightCharts.createChart(chartContainer, {
                width: chartContainer.clientWidth,
                height: 600,
                layout: {
                    background: { color: 'transparent' },
                    textColor: 'rgba(255, 255, 255, 0.9)',
                },
                grid: {
                    vertLines: { color: 'rgba(255, 255, 255, 0.1)' },
                    horzLines: { color: 'rgba(255, 255, 255, 0.1)' },
                },
                crosshair: {
                    mode: LightweightCharts.CrosshairMode.Normal,
                },
                rightPriceScale: {
                    borderColor: 'rgba(255, 255, 255, 0.2)',
                },
                timeScale: {
                    borderColor: 'rgba(255, 255, 255, 0.2)',
                    timeVisible: true,
                    secondsVisible: false,
                },
            });

            console.log('Main chart created, adding candlestick series...');

            this.candlestickSeries = this.mainChart.addCandlestickSeries({
                upColor: '#22c55e',
                downColor: '#ef4444',
                borderVisible: false,
                wickUpColor: '#22c55e',
                wickDownColor: '#ef4444',
            });

            console.log('Candlestick series added');
            this.seriesMap.set('candlestick', this.candlestickSeries);

            // Volume chart
            this.volumeChart = LightweightCharts.createChart(volumeContainer, {
                width: volumeContainer.clientWidth,
                height: 200,
                layout: {
                    background: { color: 'transparent' },
                    textColor: 'rgba(255, 255, 255, 0.9)',
                },
                grid: {
                    vertLines: { color: 'rgba(255, 255, 255, 0.1)' },
                    horzLines: { color: 'rgba(255, 255, 255, 0.1)' },
                },
                rightPriceScale: {
                    borderColor: 'rgba(255, 255, 255, 0.2)',
                    scaleMargins: {
                        top: 0.1,
                        bottom: 0.1,
                    },
                },
                timeScale: {
                    borderColor: 'rgba(255, 255, 255, 0.2)',
                    visible: true,
                    timeVisible: true,
                    secondsVisible: false,
                },
                crosshair: {
                    mode: LightweightCharts.CrosshairMode.Normal,
                    vertLine: {
                        color: 'rgba(255, 255, 255, 0.3)',
                        width: 1,
                        style: 1,
                    },
                    horzLine: {
                        color: 'rgba(255, 255, 255, 0.3)',
                        width: 1,
                        style: 1,
                    },
                },
            });

            this.volumeSeries = this.volumeChart.addHistogramSeries({
                priceFormat: {
                    type: 'volume',
                },
                priceScaleId: '',
                scaleMargins: {
                    top: 0.1,
                    bottom: 0,
                },
            });

            // Indicator chart
            this.indicatorChart = LightweightCharts.createChart(indicatorContainer, {
                width: indicatorContainer.clientWidth,
                height: 200,
                layout: {
                    background: { color: 'transparent' },
                    textColor: 'rgba(255, 255, 255, 0.9)',
                },
                grid: {
                    vertLines: { color: 'rgba(255, 255, 255, 0.1)' },
                    horzLines: { color: 'rgba(255, 255, 255, 0.1)' },
                },
                rightPriceScale: {
                    borderColor: 'rgba(255, 255, 255, 0.2)',
                    scaleMargins: {
                        top: 0.1,
                        bottom: 0.1,
                    },
                },
                timeScale: {
                    borderColor: 'rgba(255, 255, 255, 0.2)',
                    visible: true,
                    timeVisible: true,
                    secondsVisible: false,
                },
                crosshair: {
                    mode: LightweightCharts.CrosshairMode.Normal,
                },
            });

            // FIXED: Improved crosshair synchronization without causing errors
            this.mainChart.subscribeCrosshairMove((param) => {
                if (param.time) {
                    try {
                        // Sync volume chart crosshair
                        this.volumeChart.setCrosshairPosition(
                            param.point?.y || 0,
                            param.time,
                            this.volumeSeries
                        );
                    } catch (e) {
                        console.debug('Could not sync volume crosshair:', e.message);
                    }
                }
            });

            this.volumeChart.subscribeCrosshairMove((param) => {
                if (param.time) {
                    try {
                        // Sync main chart crosshair
                        this.mainChart.setCrosshairPosition(
                            param.point?.y || 0,
                            param.time,
                            this.candlestickSeries
                        );
                    } catch (e) {
                        console.debug('Could not sync main crosshair:', e.message);
                    }
                }
            });

            // Handle resize
            const resizeObserver = new ResizeObserver(() => {
                if (this.mainChart && chartContainer) {
                    this.mainChart.applyOptions({ width: chartContainer.clientWidth });
                }
                if (this.volumeChart && volumeContainer) {
                    this.volumeChart.applyOptions({ width: volumeContainer.clientWidth });
                }
                if (this.indicatorChart && indicatorContainer) {
                    this.indicatorChart.applyOptions({ width: indicatorContainer.clientWidth });
                }
            });

            resizeObserver.observe(chartContainer);

            console.log('All charts initialized successfully');

        } catch (error) {
            console.error('Error in setupCharts:', error);
            throw error;
        }
    }

    bindEvents() {
        const symbolSelect = document.getElementById('symbolSelect');
        const chartTypeSelect = document.getElementById('chartType');
        const refreshBtn = document.getElementById('refreshBtn');

        if (symbolSelect) {
            symbolSelect.addEventListener('change', (e) => {
                this.selectedSymbol = e.target.value;
                if (this.selectedSymbol) {
                    this.loadChartData();
                }
            });
        }

        if (chartTypeSelect) {
            chartTypeSelect.addEventListener('change', (e) => {
                this.chartType = e.target.value;
                this.updateChartType();
            });
        }

        if (refreshBtn) {
            refreshBtn.addEventListener('click', () => {
                this.loadChartData();
            });
        }

        // NEW: Bind indicator checkboxes
        Object.keys(this.indicators).forEach(indicator => {
            const checkbox = document.getElementById(indicator);
            if (checkbox) {
                checkbox.addEventListener('change', (e) => {
                    this.indicators[indicator] = e.target.checked;
                    console.log(`Indicator ${indicator} toggled:`, e.target.checked);
                    
                    // Show/hide indicator chart area based on RSI or MACD
                    const indicatorArea = document.getElementById('indicatorChart');
                    if (this.indicators.rsi || this.indicators.macd) {
                        indicatorArea.classList.add('show');
                    } else {
                        indicatorArea.classList.remove('show');
                    }
                    
                    // Update indicators if data is loaded
                    if (this.currentData) {
                        this.updateIndicators();
                    }
                });
            }
        });
    }

    async loadChartData() {
        if (!this.selectedSymbol) {
            console.warn('No symbol selected');
            return;
        }

        if (!this.candlestickSeries || !this.volumeSeries) {
            console.error('Charts not initialized before loading data');
            this.showMessage('Charts are still initializing. Please wait...', 'warning');
            return;
        }

        try {
            const params = new URLSearchParams({
                symbol: this.selectedSymbol
            });

            const response = await fetch(`/trading/api/chart-data?${params}`);
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const data = await response.json();

            if (!data || !data.data || data.data.length === 0) {
                console.warn('No chart data available');
                this.showMessage('No data available for this symbol', 'warning');
                return;
            }

            console.log(`Loaded ${data.data.length} data points for ${this.selectedSymbol}`);
            this.renderChartData(data);
            this.updateStockInfo(data);
        } catch (error) {
            console.error('Error loading chart data:', error);
            this.showMessage(`Failed to load chart data: ${error.message}`, 'error');
        }
    }

    updateChartType() {
        if (!this.mainChart) return;

        try {
            if (this.candlestickSeries) {
                this.mainChart.removeSeries(this.candlestickSeries);
            }

            switch(this.chartType) {
                case 'line':
                    this.candlestickSeries = this.mainChart.addLineSeries({
                        color: '#3b82f6',
                        lineWidth: 2,
                    });
                    break;
                case 'area':
                    this.candlestickSeries = this.mainChart.addAreaSeries({
                        topColor: 'rgba(59, 130, 246, 0.4)',
                        bottomColor: 'rgba(59, 130, 246, 0.0)',
                        lineColor: '#3b82f6',
                        lineWidth: 2,
                    });
                    break;
                case 'bar':
                    this.candlestickSeries = this.mainChart.addBarSeries({
                        upColor: '#22c55e',
                        downColor: '#ef4444',
                    });
                    break;
                default:
                    this.candlestickSeries = this.mainChart.addCandlestickSeries({
                        upColor: '#22c55e',
                        downColor: '#ef4444',
                        borderVisible: false,
                        wickUpColor: '#22c55e',
                        wickDownColor: '#ef4444',
                    });
            }

            // FIXED: Re-render data with proper format for the new chart type
            if (this.currentData) {
                this.renderChartDataForType(this.currentData);
            }
        } catch (error) {
            console.error('Error updating chart type:', error);
        }
    }

    // FIXED: New method to render data based on chart type
    renderChartDataForType(data) {
        if (!this.candlestickSeries) {
            console.error('Chart series not initialized');
            return;
        }

        try {
            // For line and area charts, we need {time, value} format
            // For candlestick and bar charts, we need {time, open, high, low, close} format
            
            if (this.chartType === 'line' || this.chartType === 'area') {
                // Convert to line/area format using close price
                const lineData = data.data
                    .filter(item => {
                        return item.date && 
                               !isNaN(item.close) && 
                               item.close > 0;
                    })
                    .map(item => {
                        const timestamp = new Date(item.date + 'T00:00:00Z').getTime() / 1000;
                        
                        return {
                            time: timestamp,
                            value: item.close  // Use close price for line/area
                        };
                    })
                    .sort((a, b) => a.time - b.time);

                console.log('Line/Area data points:', lineData.length);
                
                if (lineData.length === 0) {
                    throw new Error('No valid line/area data after filtering');
                }

                this.candlestickSeries.setData(lineData);
            } else {
                // Candlestick or bar format
                const candleData = data.data
                    .filter(item => {
                        return item.date && 
                               !isNaN(item.open) && 
                               !isNaN(item.high) && 
                               !isNaN(item.low) && 
                               !isNaN(item.close) &&
                               item.open > 0 && 
                               item.high > 0 && 
                               item.low > 0 && 
                               item.close > 0;
                    })
                    .map(item => {
                        const timestamp = new Date(item.date + 'T00:00:00Z').getTime() / 1000;
                        
                        return {
                            time: timestamp,
                            open: item.open,
                            high: item.high,
                            low: item.low,
                            close: item.close
                        };
                    })
                    .sort((a, b) => a.time - b.time);

                console.log('Candlestick/Bar data points:', candleData.length);
                
                if (candleData.length === 0) {
                    throw new Error('No valid candlestick/bar data after filtering');
                }

                this.candlestickSeries.setData(candleData);
            }

            try {
                this.mainChart.timeScale().fitContent();
            } catch (e) {
                console.debug('Could not fit content:', e.message);
            }

        } catch (error) {
            console.error('Error rendering chart data for type:', error);
            this.showMessage('Error displaying chart: ' + error.message, 'error');
            throw error;
        }
    }

    // FIXED: Update renderChartData to use the original format for initial load
    renderChartData(data) {
        if (!this.candlestickSeries || !this.volumeSeries) {
            console.error('Chart series not initialized');
            throw new Error('Charts not properly initialized');
        }

        try {
            const candleData = data.data
                .filter(item => {
                    return item.date && 
                           !isNaN(item.open) && 
                           !isNaN(item.high) && 
                           !isNaN(item.low) && 
                           !isNaN(item.close) &&
                           item.open > 0 && 
                           item.high > 0 && 
                           item.low > 0 && 
                           item.close > 0;
                })
                .map(item => {
                    const timestamp = new Date(item.date + 'T00:00:00Z').getTime() / 1000;
                    
                    return {
                        time: timestamp,
                        open: item.open,
                        high: item.high,
                        low: item.low,
                        close: item.close
                    };
                })
                .sort((a, b) => a.time - b.time);

            const volumeData = data.data
                .filter(item => {
                    return item.date && 
                           !isNaN(item.open) && 
                           !isNaN(item.close);
                })
                .map(item => {
                    const timestamp = new Date(item.date + 'T00:00:00Z').getTime() / 1000;
                    
                    const isUp = item.close >= item.open;
                    
                    let volumeValue = item.volume || 0;
                    
                    if (volumeValue < 0.01) {
                        const priceRange = Math.abs(item.high - item.low);
                        volumeValue = priceRange * 100;
                    }
                    
                    return {
                        time: timestamp,
                        value: volumeValue,
                        color: isUp 
                            ? 'rgba(34, 197, 94, 0.6)'
                            : 'rgba(239, 68, 68, 0.6)'
                    };
                })
                .sort((a, b) => a.time - b.time);

            console.log('Processed candle data points:', candleData.length);
            console.log('Processed volume data points:', volumeData.length);

            if (candleData.length === 0) {
                throw new Error('No valid candle data after filtering');
            }

            this.candlestickSeries.setData(candleData);
            this.volumeSeries.setData(volumeData);

            // FIXED: Improved time scale synchronization with safety checks
            try {
                this.mainChart.timeScale().fitContent();
                this.volumeChart.timeScale().fitContent();
                
                // FIXED: Remove problematic synchronization that causes "Value is null" error
                // The charts will handle their own time scales independently
                
            } catch (e) {
                console.debug('Could not fit content:', e.message);
            }

            this.currentData = data;
            
            // Log volume statistics for debugging
            if (volumeData.length > 0) {
                const volumes = volumeData.map(v => v.value);
                const maxVol = Math.max(...volumes);
                const minVol = Math.min(...volumes);
                const avgVol = volumes.reduce((a, b) => a + b, 0) / volumes.length;
                console.log('Volume stats - Max:', maxVol.toFixed(2), 'Min:', minVol.toFixed(2), 'Avg:', avgVol.toFixed(2));
            }
        } catch (error) {
            console.error('Error rendering chart data:', error);
            this.showMessage('Error displaying chart: ' + error.message, 'error');
            throw error;
        }
    }

    updateStockInfo(data) {
        if (!data.data || data.data.length === 0) return;

        const latestData = data.data[data.data.length - 1];
        const previousData = data.data.length > 1 ? data.data[data.data.length - 2] : null;

        document.getElementById('stockSymbol').textContent = data.symbol;
        document.getElementById('openPrice').textContent = latestData.open.toFixed(2);
        document.getElementById('highPrice').textContent = latestData.high.toFixed(2);
        document.getElementById('lowPrice').textContent = latestData.low.toFixed(2);
        document.getElementById('closePrice').textContent = latestData.close.toFixed(2);
        document.getElementById('volume').textContent = this.formatVolume(latestData.volume);

        if (previousData) {
            const change = latestData.close - previousData.close;
            const changePercent = (change / previousData.close) * 100;
            const changeElement = document.getElementById('priceChange');
            
            changeElement.textContent = `${change > 0 ? '+' : ''}${change.toFixed(2)} (${changePercent.toFixed(2)}%)`;
            changeElement.className = 'stat-value ' + (change >= 0 ? 'positive' : 'negative');
        }
    }

    formatVolume(volume) {
        if (volume >= 1000000) {
            return (volume / 1000000).toFixed(2) + 'M';
        } else if (volume >= 1000) {
            return (volume / 1000).toFixed(2) + 'K';
        }
        return volume.toFixed(0);
    }

    showMessage(message, type = 'info') {
        let messageEl = document.getElementById('chart-message');
        if (!messageEl) {
            messageEl = document.createElement('div');
            messageEl.id = 'chart-message';
            messageEl.style.cssText = 'position: fixed; top: 120px; left: 50%; transform: translateX(-50%); padding: 1rem 2rem; border-radius: 12px; z-index: 1000; min-width: 300px; text-align: center;';
            document.body.appendChild(messageEl);
        }

        const colors = {
            info: { bg: 'rgba(59, 130, 246, 0.15)', border: 'rgba(59, 130, 246, 0.3)' },
            warning: { bg: 'rgba(245, 158, 11, 0.15)', border: 'rgba(245, 158, 11, 0.3)' },
            error: { bg: 'rgba(239, 68, 68, 0.15)', border: 'rgba(239, 68, 68, 0.3)' }
        };

        const color = colors[type] || colors.info;
        messageEl.style.background = color.bg;
        messageEl.style.border = `1px solid ${color.border}`;
        messageEl.style.color = 'rgba(255, 255, 255, 0.9)';
        messageEl.textContent = message;
        messageEl.style.display = 'block';

        setTimeout(() => {
            messageEl.style.display = 'none';
        }, 3000);
    }

    // Re-add updateIndicators and related methods from previous implementation
    updateIndicators() {
        if (!this.currentData || !this.mainChart) return;

        try {
            const indicators = this.currentData.indicators;

            // Clear existing indicator series (except main candlestick)
            this.seriesMap.forEach((series, key) => {
                if (key !== 'candlestick' && key !== 'volume') {
                    try {
                        this.mainChart.removeSeries(series);
                    } catch (e) {
                        // Series already removed
                    }
                }
            });
            this.seriesMap.clear();
            this.seriesMap.set('candlestick', this.candlestickSeries);

            // Add SMA lines
            if (this.indicators.sma20 && indicators.sma20) {
                this.addLineSeries('SMA 20', indicators.sma20, '#f59e0b');
            }

            if (this.indicators.sma50 && indicators.sma50) {
                this.addLineSeries('SMA 50', indicators.sma50, '#8b5cf6');
            }

            if (this.indicators.ema12 && indicators.ema12) {
                this.addLineSeries('EMA 12', indicators.ema12, '#10b981');
            }

            if (this.indicators.ema26 && indicators.ema26) {
                this.addLineSeries('EMA 26', indicators.ema26, '#ef4444');
            }

            // Bollinger Bands
            if (this.indicators.bollinger && indicators.bollingerBands) {
                this.addLineSeries('BB Upper', indicators.bollingerBands.upper, '#94a3b8', 1);
                this.addLineSeries('BB Middle', indicators.bollingerBands.middle, '#64748b', 1);
                this.addLineSeries('BB Lower', indicators.bollingerBands.lower, '#94a3b8', 1);
            }

            // RSI
            if (this.indicators.rsi && indicators.rsi) {
                this.renderRSI(indicators.rsi);
            } else if (this.indicatorChart && !this.indicators.macd) {
                this.clearIndicatorChart();
            }

            // MACD
            if (this.indicators.macd && indicators.macd) {
                this.renderMACD(indicators.macd);
            } else if (this.indicatorChart && !this.indicators.rsi) {
                this.clearIndicatorChart();
            }
        } catch (error) {
            console.error('Error updating indicators:', error);
        }
    }

    addLineSeries(title, data, color, lineWidth = 2) {
        if (!this.mainChart || !this.currentData) return;

        try {
            const lineData = data
                .map((value, index) => {
                    const item = this.currentData.data[index];
                    if (!item || !item.date || value === null || value === undefined || isNaN(value)) {
                        return null;
                    }
                    
                    const timestamp = new Date(item.date + 'T00:00:00Z').getTime() / 1000;
                    
                    return {
                        time: timestamp,
                        value: value
                    };
                })
                .filter(item => item !== null)
                .sort((a, b) => a.time - b.time);

            if (lineData.length > 0) {
                const series = this.mainChart.addLineSeries({
                    color: color,
                    lineWidth: lineWidth,
                    title: title
                });
                series.setData(lineData);
                this.seriesMap.set(title, series);
            }
        } catch (error) {
            console.error(`Error adding ${title} line series:`, error);
        }
    }

    clearIndicatorChart() {
        if (!this.indicatorChart) return;
        
        try {
            this.indicatorSeriesList.forEach(series => {
                try {
                    this.indicatorChart.removeSeries(series);
                } catch (e) {
                    // Already removed
                }
            });
            this.indicatorSeriesList = [];
        } catch (error) {
            console.debug('Error clearing indicator chart:', error.message);
        }
    }

    renderRSI(rsiData) {
        if (!this.indicatorChart || !this.currentData) return;

        try {
            this.clearIndicatorChart();

            const data = rsiData
                .map((value, index) => {
                    const item = this.currentData.data[index];
                    if (!item || !item.date || value === null || value === undefined || isNaN(value)) {
                        return null;
                    }
                    
                    const timestamp = new Date(item.date + 'T00:00:00Z').getTime() / 1000;
                    
                    return {
                        time: timestamp,
                        value: Math.max(0, Math.min(100, value))
                    };
                })
                .filter(item => item !== null)
                .sort((a, b) => a.time - b.time);

            if (data.length === 0) {
                console.warn('No valid RSI data to display');
                return;
            }

            const rsiSeries = this.indicatorChart.addLineSeries({
                color: '#3b82f6',
                lineWidth: 2,
            });
            rsiSeries.setData(data);
            this.indicatorSeriesList.push(rsiSeries);

            // Add reference lines at 30 and 70
            this.addRSIReferenceLine(30, '#ef4444');
            this.addRSIReferenceLine(70, '#22c55e');

            try {
                this.indicatorChart.timeScale().fitContent();
            } catch (e) {
                console.debug('Could not fit RSI content:', e.message);
            }
        } catch (error) {
            console.error('Error rendering RSI:', error);
        }
    }

    addRSIReferenceLine(value, color) {
        if (!this.indicatorChart || !this.currentData) return;

        try {
            const lineData = this.currentData.data
                .filter(item => item.date)
                .map(item => ({
                    time: new Date(item.date + 'T00:00:00Z').getTime() / 1000,
                    value: value
                }))
                .sort((a, b) => a.time - b.time);

            if (lineData.length > 0) {
                const line = this.indicatorChart.addLineSeries({
                    color: color,
                    lineWidth: 1,
                    lineStyle: 2,
                });
                line.setData(lineData);
                this.indicatorSeriesList.push(line);
            }
        } catch (e) {
            console.debug('Could not add RSI reference line:', e.message);
        }
    }

    renderMACD(macdData) {
        if (!this.indicatorChart || !this.currentData) return;

        try {
            this.clearIndicatorChart();

            const processMACD = (values, name) => {
                return values
                    .map((value, index) => {
                        const item = this.currentData.data[index];
                        if (!item || !item.date || value === null || value === undefined || isNaN(value)) {
                            return null;
                        }
                        
                        const timestamp = new Date(item.date + 'T00:00:00Z').getTime() / 1000;
                        
                        return {
                            time: timestamp,
                            value: value
                        };
                    })
                    .filter(item => item !== null)
                    .sort((a, b) => a.time - b.time);
            };

            const macdLine = processMACD(macdData.macd, 'MACD');
            const signalLine = processMACD(macdData.signal, 'Signal');
            const histogram = macdData.histogram
                .map((value, index) => {
                    const item = this.currentData.data[index];
                    if (!item || !item.date || value === null || value === undefined || isNaN(value)) {
                        return null;
                    }
                    
                    const timestamp = new Date(item.date + 'T00:00:00Z').getTime() / 1000;
                    
                    return {
                        time: timestamp,
                        value: value,
                        color: value >= 0 ? '#22c55e80' : '#ef444480'
                    };
                })
                .filter(item => item !== null)
                .sort((a, b) => a.time - b.time);

            if (histogram.length === 0 || macdLine.length === 0 || signalLine.length === 0) {
                console.warn('Insufficient MACD data to display');
                return;
            }

            // Add histogram first (background)
            const histogramSeries = this.indicatorChart.addHistogramSeries({
                priceFormat: {
                    type: 'price',
                },
            });
            histogramSeries.setData(histogram);
            this.indicatorSeriesList.push(histogramSeries);

            // Add MACD line
            const macdSeries = this.indicatorChart.addLineSeries({
                color: '#3b82f6',
                lineWidth: 2,
            });
            macdSeries.setData(macdLine);
            this.indicatorSeriesList.push(macdSeries);

            // Add signal line
            const signalSeries = this.indicatorChart.addLineSeries({
                color: '#ef4444',
                lineWidth: 2,
            });
            signalSeries.setData(signalLine);
            this.indicatorSeriesList.push(signalSeries);

            try {
                this.indicatorChart.timeScale().fitContent();
            } catch (e) {
                console.debug('Could not fit MACD content:', e.message);
            }
        } catch (error) {
            console.error('Error rendering MACD:', error);
        }
    }
}

// Initialize chart when DOM is ready AND library is loaded
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeChart);
} else {
    initializeChart();
}