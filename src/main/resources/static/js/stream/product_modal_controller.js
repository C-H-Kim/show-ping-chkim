document.addEventListener("DOMContentLoaded", function () {
    const selectBtn = document.querySelector(".select-btn");
    const productModal = document.getElementById("productModal");
    const closeBtn = document.querySelector(".close");
    const container = document.querySelector(".product-list-container");

    if (!selectBtn || !productModal || !closeBtn || !container) return;

    const productList = productModal.querySelector(".product-list");
    const productElement = document.querySelector(".product-element");

    if (!productList || !productElement) return;

    // loading & end message
    const loadingEl = document.getElementById("loading");
    const endMsgEl = document.getElementById("end-msg");

    // search input
    const searchBar = document.querySelector(".search-bar");
    const searchInput = searchBar ? searchBar.querySelector('input[type="search"], input') : null;
    const searchBtn = document.querySelector(".search-btn");

    // constant variable
    const LIST_API = "/api/live/product/list";
    const SEARCH_API = "/api/live/search-product";
    const SIZE = 20;
    const MIN_LEN = 2;

    // state value
    const state = {
        mode: "list",   // "list" | "search"
        keyword: "",    // current keyword
        loading: false,
        hasNext: true,
        lastProductNo: null,
        token: 0,
        loadingOwner: null, // "list" | "search"
    };

    const debounce = (fn, delay = 300) => {
        let timer = null;
        return (...args) => {
            clearTimeout(timer);
            timer = setTimeout(() => fn(...args), delay);
        };
    };

    function nearBottom() {
        const {scrollTop, clientHeight, scrollHeight} = container;
        return scrollTop + clientHeight >= scrollHeight - 10;
    }

    // controll loading message
    function showLoading(owner) {
        if (!loadingEl) return;
        state.loadingOwner = owner;
        loadingEl.style.display = "block";
    }

    function hideLoading(owner) {
        if (!loadingEl) return;

        // owner가 현재 주도권일 때만 끈다
        if (state.loadingOwner === owner) {
            loadingEl.style.display = "none";
            state.loadingOwner = null;
        }
    }

    // controll end message
    function showEndMsg() {
        if (endMsgEl) {
            endMsgEl.style.display = "block";
        }
    }

    function hideEndMsg() {
        if (endMsgEl) {
            endMsgEl.style.display = "none";
        }
    }

    function setEndMsg(text) {
        if (!endMsgEl) return;
        endMsgEl.textContent = text;
    }

    // controll reset
    function clearList() {
        productList.innerHTML = "";
        hideEndMsg();
    }

    function clearSearchInput() {
        if (searchInput) searchInput.value = "";
    }

    function resetCommon() {
        state.loading = false;
        state.hasNext = true;
        state.lastProductNo = null;
        state.token += 1;
        clearList()
        hideLoading("list");
        hideLoading("search");
    }

    // switching mode (list || search)
    function switchToListAndLoad() {
        resetCommon();
        state.mode = "list";
        container.scrollTop = 0;
        loadMore();
    }

    function switchToSearchAndLoad(keyword) {
        resetCommon();
        state.mode = "search";
        state.keyword = keyword;
        container.scrollTop = 0;
        loadMore();
    }

    // ==== Data load ====

    async function loadMore() {
        if (state.loading || !state.hasNext) return;

        if (state.mode === "search") {
            const kw = (state.keyword || "").trim();

            // 검색어가 짧아졌으면 전체 리스트로 복귀
            if (kw.length < MIN_LEN) {
                switchToListAndLoad();
                return;
            }

            await loadMoreSearch(kw);
            return;
        }

        // default
        await loadMoreList();
    }

    async function loadMoreList() {
        if (state.mode !== "list") return;
        if (state.loading || !state.hasNext) return;

        state.loading = true;
        const myToken = state.token;

        showLoading("list");

        try {
            const response = await axios.get(LIST_API, {
                params: {lastProductNo: state.lastProductNo, size: SIZE},
                withCredentials: true,
            });

            if (state.mode !== "list" || myToken !== state.token) return;

            const {data, hasNext, nextLastProductNo} = response.data;

            renderProducts(data || []);
            state.hasNext = !!hasNext;
            state.lastProductNo = nextLastProductNo ?? null;

            if (!state.hasNext){
                setEndMsg("마지막 상품입니다.")
                showEndMsg();
            }
        } catch (err) {
            console.log(err);
        } finally {
            if (state.mode === "list" && myToken === state.token) {
                hideLoading("list");
                state.loading = false;
            }
        }
    }

    async function loadMoreSearch(keyword) {
        if (state.mode !== "search") return;
        if (state.loading || !state.hasNext) return;

        state.loading = true;
        const myToken = state.token;

        showLoading("search");

        try {
            const response = await axios.get(SEARCH_API, {
                params: {keyword, lastProductNo: state.lastProductNo, size: SIZE},
                withCredentials: true,
            });

            if (state.mode !== "search" || myToken !== state.token) return;

            const {data, hasNext, nextLastProductNo} = response.data;

            // 첫 페이지인데 검색 결과가 없는 경우
            const isFirstPage = state.lastProductNo == null;
            if (isFirstPage && (!data || data.length === 0)) {
                state.hasNext = false;
                state.lastProductNo = null;

                hideLoading("search");
                hideLoading("list");
                state.loading = false;

                setEndMsg("검색된 상품이 없습니다.");
                showEndMsg();

                return;
            }

            renderProducts(data || []);
            state.hasNext = !!hasNext;
            state.lastProductNo = nextLastProductNo ?? null;

            if (!state.hasNext) {
                setEndMsg("마지막 상품입니다.");
                showEndMsg();
            }
        } catch (err) {
            console.log("search failed: ", err);
        } finally {
            if (state.mode === "search" && myToken === state.token) {
                console.log("loadMoreSearch finally 부분")
                hideLoading("search");
                state.loading = false;
            }
        }
    }

    // render products
    function renderProducts(data) {
        data.forEach((product) => {
            productList.appendChild(createProductItem(product));
        });
    }

    function createProductItem(product) {
        const li = document.createElement("li");
        li.className = "product-item";
        li.id = product.productNo;

        li.innerHTML = `
            <img src="${product.productImg}" alt="상품 이미지">
            <div class="product-info">
                <div class="product-name">${product.productName}</div>
                <div class="price-container">
                    <span class="original-price">${Number(product.productPrice).toLocaleString()}원</span>
                </div>
            </div>
        `;

        li.addEventListener("click", () => selectProduct(li));

        return li;
    }

    // render selected product
    function selectProduct(item) {
        const imgEl = item.querySelector("img");
        const nameEl = item.querySelector(".product-name");
        const priceEl = item.querySelector(".original-price");

        const imgSrc = imgEl ? imgEl.src : "";
        const name = nameEl ? nameEl.textContent.trim() : "";
        const originalPrice = priceEl ? priceEl.textContent.trim() : "";
        const productNo = item.id;

        // 선택된 상품 표시 영역 내부를 동적으로 구성
        productElement.innerHTML = `
            <img src="${imgSrc}" alt="상품 이미지" class="product-img">
            <div class="product-info" id="${productNo}">
                <p class="product-name">${name}</p>
                <div class="product-price-container">
                    <p class="product-origin-price">${originalPrice}</p>
                </div>
            </div>
        `;

        productModal.style.display = "none";

        clearSearchInput();
        state.keyword = "";
        state.mode = "list";

        // 다음 모달 오픈 때 안정적으로 첫 페이지부터 로드되도록 상태 초기화
        resetCommon();
    }

    // ==== Event bindings ====

    // Open modal
    selectBtn.addEventListener("click", () => {
        productModal.style.display = "block";

        const keyword = searchInput ? searchInput.value.trim() : "";
        if (searchInput && keyword.length >= MIN_LEN) {
            switchToSearchAndLoad(keyword);
        } else {
            switchToListAndLoad();
        }
    });

    // Close modal
    closeBtn.addEventListener("click", () => {
        productModal.style.display = "none";

        clearSearchInput();
        state.keyword = "";
        state.mode = "list";

        resetCommon();
    });

    // Scroll listener
    container.addEventListener("scroll", () => {
        if (state.loading || !state.hasNext) return;
        if (nearBottom()) loadMore();
    })

    // Search icon button click
    if (searchBtn) {
        searchBtn.addEventListener("click", (e) => {
            e.preventDefault();

            const keyword = searchInput ? searchInput.value.trim() : "";
            if (keyword.length >= MIN_LEN) {
                switchToSearchAndLoad(keyword);
            } else {
                switchToListAndLoad();
            }
        });
    }

    // Search input listeners
    if (searchInput) {
        const onInput = debounce(() => {
            const keyword = searchInput.value.trim();

            if (keyword.length >= MIN_LEN) {
                switchToSearchAndLoad(keyword);
            } else {
                switchToListAndLoad();
            }
        }, 300);

        searchInput.addEventListener("input", onInput);

        searchInput.addEventListener("keydown", (e) => {
            if (e.key !== "Enter") return;
            e.preventDefault();

            const keyword = searchInput.value.trim();
            if (keyword.length >= MIN_LEN) {
                switchToSearchAndLoad(keyword);
            } else {
                switchToListAndLoad();
            }
        });
    }
});