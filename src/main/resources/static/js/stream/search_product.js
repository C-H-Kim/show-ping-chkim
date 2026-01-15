document.addEventListener("DOMContentLoaded", function () {
    const searchBar = document.querySelector(".search-bar");
    if (!searchBar) {
        return;
    }
    const input = searchBar.querySelector('input[type="search"], input');
    if (!input) {
        return;
    }
    const productModal = document.getElementById("productModal"); // 모달 영역
    const container = document.querySelector(".product-list-container"); // 상품 목록 영역
    const productList = productModal.querySelector(".product-list"); // 상품 목록
    const loadingEl = document.getElementById("loading"); // 로딩 표시
    const endMsgEl = document.getElementById("end-msg"); // 상품 목록 끝 메시지
    const productElement = document.querySelector(".product-element"); // 선택된 상품 영역

    const SEARCH_API = "/api/live/search-product";

    const size = 20;
    const MIN_LEN = 2;

    let loading = false;
    let hasNext = true;
    let lastProductNo = null;
    let currentKeyword = "";
    // let abortController = null;

    let requestSeq = 0;

    const debounce = (fn, delay = 300) => {
        let timer = null;
        return (...args) => {
            clearTimeout(timer);
            timer = setTimeout(() => fn(...args), delay);
        }
    }

    // 검색어 변경 시 상태/리스트 초기화
    function resetSearchState(keyword) {
        currentKeyword = keyword;
        loading = false;
        hasNext = true;
        lastProductNo = null;

        productList.innerHTML = "";
        if (endMsgEl) {
            endMsgEl.style.display = "none";
        }
        if (loadingEl) {
            loadingEl.style.display = "none";
        }
    }

    // 모달 스크롤 감지
    container.addEventListener("scroll", () => {
        const {scrollTop, clientHeight, scrollHeight} = container;
        if (!loading && hasNext && scrollTop + clientHeight >= scrollHeight - 10) {
            loadMore();
        }
    });

    async function loadMore() {
        const keyword = currentKeyword.trim();

        if (loading || !hasNext) {
            return;
        }

        // 검색어가 너무 짧으면 목록 비우고 종료
        if (keyword.length < MIN_LEN) {
            productList.innerHTML = "";
            if (endMsgEl) {
                endMsgEl.style.display = "none";
            }
            return;
        }

        loading = true;
        if (loadingEl) {
            loadingEl.style.display = "block";
        }

        const mySeq = ++requestSeq;

        try {
            const response = await axios.get(SEARCH_API, {
                params: {
                    keyword,
                    lastProductNo,
                    size,
                },
                withCredentials: true,
            });

            const {data, hasNext: more, nextLastProductNo} = response.data;

            // 더 최신 요청이 이미 시작되었으면 이 응답은 무시
            if (mySeq !== requestSeq) {
                return;
            }

            // 상품 렌더링
            data.forEach((product) => {
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

                productList.appendChild(li);
            });

            hasNext = more;
            lastProductNo = nextLastProductNo;
            console.log(lastProductNo);

            if (!hasNext && endMsgEl) {
                endMsgEl.style.display = "block";
            }
        } catch (err) {
            console.log("search failed: ", err);
        } finally {
            // 더 최신 요청이 시작된 상태면 Loading 플래그만 자연스럽게 풀리게 설정
            if (mySeq === requestSeq) {
                loading = false;
                if (loadingEl) {
                    loadingEl.style.display = "none";
                }
            }
        }
    }

    const onInputSearch = debounce(() => {
        const keyword = input.value.trim();

        // 검색어가 바뀌었으면 초기화
        resetSearchState(keyword);

        // 검색어가 유효하면 첫 로드
        if (keyword.length >= MIN_LEN) {
            loadMore();
        }
    }, 300);

    input.addEventListener("input", onInputSearch);

    // 엔터로 즉시 검색 확정
    input.addEventListener("keydown", (e) => {
        if (e.key === "Enter") {
            e.preventDefault();
            const keyword = input.value.trim();
            resetSearchState(keyword);
            if (keyword.length >= MIN_LEN) {
                loadMore();
            }
        }
    });

    function selectProduct(item) {
        const imgSrc = item.querySelector("img").src;
        const name = item.querySelector(".product-name").textContent.trim();
        const originalPrice = item.querySelector(".original-price").textContent.trim();
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

        if (productModal) {
            productModal.style.display = "none";
        }
    }
});