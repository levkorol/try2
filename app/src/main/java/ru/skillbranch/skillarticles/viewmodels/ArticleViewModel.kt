package ru.skillbranch.skillarticles.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import ru.skillbranch.skillarticles.data.ArticleData
import ru.skillbranch.skillarticles.data.ArticlePersonalInfo
import ru.skillbranch.skillarticles.data.repositories.ArticleRepository
import ru.skillbranch.skillarticles.extensions.data.toAppSettings
import ru.skillbranch.skillarticles.extensions.data.toArticlePersonalInfo
import ru.skillbranch.skillarticles.extensions.format

class ArticleViewModel (private val articleId: String) : BaseViewModel<ArticleState>(ArticleState()), IArticleViewModel{
    private val repository = ArticleRepository
    private var menuIsShown: Boolean = false

    init {
        //subscribe on mutable data
        subscribeOnDataSource(getArticleData()){ article, state ->
            article ?: return@subscribeOnDataSource null
            state.copy(
                shareLink = article.shareLink,
                title = article.title,
                author = article.author,
                category = article.category,
                categoryIcon = article.categoryIcon,
                date = article.date.format()
            )

        }

        subscribeOnDataSource(getArticleContent()){ content, state ->
            content ?: return@subscribeOnDataSource null
            state.copy(
                isLoadingContent = false,
                content = content
            )

        }

        subscribeOnDataSource(getArticlePersonalInfo()){ info, state ->
            info ?: return@subscribeOnDataSource null
            state.copy(
                isBookmark = info.isBookmark,
                isLike = info.isLike
            )
        }

        subscribeOnDataSource(repository.getAppSettings()){ settings, state ->
            state.copy(
                isDarkMode = settings.isDarkMode,
                isBigText = settings.isBigText
            )

        }

    }

    //load text from network
    override fun getArticleContent(): LiveData<List<Any>?>{
        return repository.loadArticleContent(articleId)
    }

    //load data from mdb
    override fun getArticleData() : LiveData<ArticleData?>{
        return repository.getArticle(articleId)
    }

    //load data from db
    override fun getArticlePersonalInfo(): LiveData<ArticlePersonalInfo?>{
        return repository.loadArticlePersonalInfo(articleId)
    }

    //session state
    override fun handleToggleMenu(){
        updateState { state ->
            state.copy(isShowMenu = !state.isShowMenu).also{ menuIsShown = !state.isShowMenu}
        }
    }

    override fun handleSearchMode(isSearch: Boolean) {

    }

    override fun handleSearch(query: String?) {

    }

    //app settings
    override fun handleNightMode(){
        val settings = currentState.toAppSettings()
        repository.updateSettings(settings.copy(isDarkMode = !settings.isDarkMode))
    }

    override fun handleUpText(){
        repository.updateSettings(currentState.toAppSettings().copy(isBigText = true))
    }

    override fun handleDownText(){
        repository.updateSettings(currentState.toAppSettings().copy(isBigText = false))
    }

    //personal article info
    override fun handleBookmark(){
        val info = currentState.toArticlePersonalInfo()
        repository.updateArticlePersonalInfo(info.copy(isBookmark = info.isBookmark))

        val msg = if (currentState.isBookmark) "Add to bookmarks" else "Remove from bookmarks"
        notify(Notify.TextMessage(msg))
    }


    override fun handleLike(){
        Log.d("ArticleViewModel", "handle like: ")
        val isLiked = currentState.isLike
        val toogleLike = {
            val info = currentState.toArticlePersonalInfo()
            repository.updateArticlePersonalInfo(info.copy(isLike = !info.isLike))
        }

        toogleLike()

        val msg = if(isLiked) Notify.TextMessage("Mark is Liked")
        else{
            Notify.ActionMessage(
                "Don`t like it anymore", //message
            "No, still like it",  // action label on snackbar
            toogleLike // handler function , if press "No, still like it" on snackbar, then toggle again
            )
        }

        notify(msg)

    }

    //not implemented
    override fun handleShare(){
        val msg = "Share is not implemented"
        notify(Notify.ErrorMessage(msg, "OK", null))
    }

    fun hideMenu(){
        updateState { it.copy(isShowMenu = false) }
    }

    fun showMenu(){
        updateState { it.copy(isShowMenu = menuIsShown) }
    }

    fun handleSearchQuery(query: String?){
        updateState { it.copy(searchQuery = query) }
    }

    fun handleIsSearch(isSearch: Boolean){
        updateState { it.copy(isSearch = isSearch) }
    }




}

data class ArticleState(
    val isAuth: Boolean = false,
    val isLoadingContent: Boolean = true,
    val isLoadingReviews: Boolean = true,
    val isLike : Boolean = false,
    val isBookmark: Boolean = false,
    val isShowMenu: Boolean = false,
    val isBigText: Boolean = false,
    val isDarkMode : Boolean = false, //темный режим
    val isSearch: Boolean = false,
    val searchQuery: String? = null,

    //результаты поиска (стартовая и конечная позиции)
    val searchResults : List<Pair<Int, Int>> = emptyList(),
    val searchPosition: Int = 0, //текущая позиция найденного результата
    val shareLink: String? = null, //ссылка Share
    val title: String? = null, //заголовок статьи
    val category: String? = null, //категория
    val categoryIcon: Any? = null, //иконка категории
    val date: String? = null, //дата публикации
    val author: Any? = null, //автор статьи
    val poster: String? = null, //Обложка статьи
    val content: List<Any> = emptyList(), //контент
    val reviews: List<Any> = emptyList() //отзывы
)