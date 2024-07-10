/**
 * @file
 * @author [Dhruv Pasricha](https://github.com/DhruvPasricha)
 * @brief [Heap Sort](https://en.wikipedia.org/wiki/Heapsort) implementation
 * @details
 * Heap-sort is a comparison-based sorting algorithm.
 * Heap-sort can be thought of as an improved selection sort:
 * like selection sort, heap sort divides its input into a sorted
 * and an unsorted region, and it iteratively shrinks the unsorted
 * region by extracting the largest element from it and inserting
 * it into the sorted region.
 *
 * Unlike selection sort,
 * heap sort does not waste time with a linear-time scan of the
 * unsorted region; rather, heap sort maintains the unsorted region
 * in a heap data structure to more quickly find the largest element
 * in each step.
 * Time Complexity : O(Nlog(N))
 */

#include <assert.h>   /// for assert
#include <stdio.h>    /// for IO operations
#include <stdlib.h>   /// for dynamic memory allocation
#include <time.h>     /// for random numbers generation
#include <inttypes.h> /// for unsigned int, int

/**
 * @brief Swapped two numbers using pointer
 * @param first pointer of first number
 * @param second pointer of second number
 */
void swap(int *first, int *second)
{
    int temp = *first;
    *first = *second;
    *second = temp;
}

/**
 * @brief heapifyDown Adjusts new root to the correct position in the heap
 * This heapify procedure can be thought of as building a heap from
 * the top down by successively shifting downward to establish the
 * heap property.
 * @param arr array to be sorted
 * @param size size of array
 * @return void
*/
void heapifyDown(int *arr, const unsigned int size)
{
    unsigned int i = 0;

    while (2 * i + 1 < size)
    {
        unsigned int maxChild = 2 * i + 1;

        if (2 * i + 2 < size && arr[2 * i + 2] > arr[maxChild])
        {
            maxChild = 2 * i + 2;
        }

        if (arr[maxChild] > arr[i])
        {
            swap(&arr[i], &arr[maxChild]);
            i = maxChild;
        }
        else
        {
            return;
        }
    }
}

/**
 * @brief heapifyUp Adjusts arr[i] to the correct position in the heap
 * This heapify procedure can be thought of as building a heap from
 * the bottom up by successively shifting upward to establish the
 * heap property.
 * @param arr array to be sorted
 * @param i index of the pushed element
 * @return void
*/
void heapifyUp(int *arr, unsigned int i)
{
    while (i > 0 && arr[(i - 1) / 2] < arr[i])
    {
        swap(&arr[(i - 1) / 2], &arr[i]);
        i = (i - 1) / 2;
    }
}

/**
 * @brief Heap Sort algorithm
 * @param arr array to be sorted
 * @param size size of the array
 * @returns void
 */
void heapSort(int *arr, const unsigned int size)
{
    if (size <= 1)
    {
        return;
    }

    for (unsigned int i = 0; i < size; i++)
    {
        // Pushing `arr[i]` to the heap

        /*heapifyUp Adjusts arr[i] to the correct position in the heap*/
        heapifyUp(arr, i);
    }

    for (unsigned int i = size - 1; i >= 1; i--)
    {
        // Moving current root to the end
        swap(&arr[0], &arr[i]);

        // `heapifyDown` adjusts new root to the correct position in the heap
        heapifyDown(arr, i);

    }
}

/**
 * @brief Self-test implementations
 * @returns void
 */
static void test()
{
    const unsigned int size = 10;
    int *arr = (int *)calloc(size, sizeof(int));

    /* generate size random numbers from 0 to 100 */
    for (unsigned int i = 0; i < size; i++)
    {
        arr[i] = rand() & (64 - 1);
        printf("%d ", arr[i]);
    }
    heapSort(arr, size);
    for (unsigned int i = 0; i < size - 1; ++i)
    {
        assert(arr[i] <= arr[i + 1]);
    }
    free(arr);
}

/**
 * @brief Main function
 * @returns 0 on exit
 */
int main()
{
    printf("Enter the size of the array\n");
    // Intializes random number generator
    srand(333);

    test(); // run self-test implementations
    return 0;
}