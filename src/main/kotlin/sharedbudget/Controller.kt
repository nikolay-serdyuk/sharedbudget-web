package sharedbudget

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import sharedbudget.entities.ExpenseDto

@RestController
class Controller(private val service: Service) {

    @GetMapping
    fun getExpenses(
        @RequestParam(value = "date", required = false)
        date: String? = null
    ) = service.getExpenses(date)

    @PostMapping
    fun postExpenses(
        @RequestParam(value = "expenses", required = true)
        expenses: Iterable<ExpenseDto>
    ) = service.postExpenses(expenses)
}
